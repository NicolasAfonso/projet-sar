package server;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import client.Cache;
import document.Document;
import document.I_Document;
import engine.Client;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;
public class Server implements I_ServerHandler{

	private HashMap<String,I_Document> documents ;
	private HashMap<I_Document,LockManager> locks;
	private HashMap<Integer, I_Document> docsClient ;
	private I_NioEngine nio ;
	Thread nioT ;
	ByteBuffer tmp ;
	private static final Logger logger = Logger.getLogger(NioEngine.class);
	
	public Server(String[] args)
	{
		documents = new HashMap<>();
		locks = new HashMap<>();
		docsClient = new HashMap<>();
		InetAddress addr;
		try {
			addr = InetAddress.getByName("localhost");
			nio= new NioEngine();
			int port = Integer.parseInt(args[0]);
			nio.initializeAsServer(addr, port, this);
			nioT = new Thread(nio);
			nioT.start();
			logger.info("Server start on  "+addr.toString()+":"+port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}

	public static void main(String[] args) {
		try {
			new Server(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivedMSG(byte[] data, TYPE_MSG type, SocketChannel socketChannel) {
		switch (type)
		{
		case ERROR :
			break;
		case HELLO_CLIENT : 
			receivedHelloClient(data,socketChannel);
			break;
		case LIST_FILE :
			reveivedListFile(data,socketChannel);
			break;
		case UPLOAD :
			receivedUploadClient(data,socketChannel);
			break;
		case DOWNLOAD : 
			receivedDownloadClient(data,socketChannel);
			break;
		case DELETE : 
			receivedDeleteFile(data,socketChannel);
			break;
		
		default :

		}

	}
	
	private void reveivedListFile(byte[] data, SocketChannel socketChannel) {
		logger.info("Received LIST_FILE");
//		if(documents.size()>0)
//		{
			Object[] dop = documents.values().toArray();
			List<String> a = new ArrayList<String>();
			for(Object o : dop)
			{
				a.add(((I_Document)o).getUrl());
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
				try {
					out = new ObjectOutputStream(bos);
					out.writeObject(a);
					byte[] docs = bos.toByteArray();
					out.close();
					bos.close();
					nio.send(socketChannel,docs,TYPE_MSG.ACK_LIST_FILE);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		//}
		
	}

	/**
	 * Callback used when a client delete a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedDeleteFile(byte[] data, SocketChannel socketChannel) {

		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int urlSize= tmp.getInt(0);
		byte[] urlb = new byte[urlSize];
		tmp.position(4);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);
		I_Document doc = documents.get(url);
		if(doc != null)
		{
			documents.remove(url);
			nio.push(doc,TYPE_MSG.DELETE);
			logger.info("Remove document "+doc.getUrl()+" and push clients");
		}
		else
		{
			Client c = nio.getClient(socketChannel);
			logger.warn("Received bad remove request "+ "from "+c.getId());
		}

	}

	/**
	 * Callback used when a client download a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedDownloadClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int urlSize= tmp.getInt(0);
		byte[] urlb = new byte[urlSize];
		tmp.position(4);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);
		I_Document doc = documents.get(url);
		if(doc != null)
		{
			byte[] docD = I_DocumentToByte(doc);
			nio.send(socketChannel,docD,TYPE_MSG.ACK_DOWNLOAD);
			logger.info("Send document "+doc.getUrl()+" to "+c.getId());
		}
		else
		{
			logger.warn("Received bad document request "+ "from "+c.getId());
			nio.send(socketChannel,"ERROR".getBytes(),TYPE_MSG.ERROR);
		}
	}

	/**
	 * Callback used when a client upload a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedUploadClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		logger.info("Received UPLOAD form "+c.getId());
		I_Document docReceived = bytesToI_Document(data);
		I_Document doc = documents.get(docReceived.getUrl()); // on suppose que c'est juste un objet pour l'instant
		if(doc == null)
		{
			logger.info("Create new document "+docReceived.getUrl()+" and push clients ");
			documents.put(docReceived.getUrl(),docReceived);
			docsClient.put(nio.getClient(socketChannel).getId(), docReceived);
			nio.push(docReceived,TYPE_MSG.PUSH_NEW_FILE);
		}
		else
		{
			if(doc.getVersionNumber() <= docReceived.getVersionNumber())
			{
				logger.info("Received : "+ docReceived.getOwner() +"-"+docReceived.getUrl()+"-"+docReceived.getVersionNumber() );
				doc.setFile(docReceived.getFile());
				doc.setVersionNumber(docReceived.getVersionNumber()+1);
				documents.put(doc.getUrl(),doc);
			}
			else
			{
				logger.warn("Received old version document "+ docReceived.getUrl() +"from "+c.getId());
			}
		}
	}
	
	/**
	 * Callback used when a new client is connected on the server content
	 * @param data
	 * @param socketChannel
	 */
	private void receivedHelloClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		tmp = ByteBuffer.allocate(4);
		tmp.put(data);
		tmp.rewind();
		int i = tmp.getInt();
		c.setId(i);
		nio.send(socketChannel, data, TYPE_MSG.ACK_HELLO_CLIENT);
		logger.info("Received HELLO_CLIENT form "+c.getId());
	}
	
	
	/*
	 * Tools
	 */
	/**
	 * Transform a bytes array in I_Document object
	 * @param data
	 * @return
	 */
	private I_Document bytesToI_Document(byte[] data){
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			I_Document doc = (I_Document) in.readObject(); 
			bis.close();
			in.close();
			return doc;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Transform a I_Document in bytes array
	 * @param doc
	 * @return
	 */
	private byte[] I_DocumentToByte(I_Document doc){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(doc);
			byte[] bytes = bos.toByteArray();
			out.close();
			bos.close();
			return bytes;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
