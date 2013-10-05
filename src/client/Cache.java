
package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.plaf.SliderUI;

import document.Document;
import document.I_Document;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;

public class Cache implements I_CacheHandler{

	private int id ; 
	private List<String> urls ;
	private I_NioEngine nio ;
	ByteBuffer tmp ;
	private String[] args;
	Thread nioT ;
	
	public Cache(String[] a)
	{
		id = Integer.parseInt(a[0]) ;
		nio = new NioEngine();
		urls =  new ArrayList<String>();
		args = a;
		
		nioT = new Thread(nio);

		//init(args);

	}

	public void init(String[] args){
		InetAddress addr;
		try {
			// Connection
			addr = InetAddress.getByName("localhost");		
			nio.initializeAsClient(addr,Integer.parseInt(args[1]),this);

			// send client id to server
			tmp = ByteBuffer.allocate(4);
			tmp.putInt(id);
			nio.send(tmp.array(), TYPE_MSG.HELLO_CLIENT);
			nioT.start();


		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}


	@Override
	public void receivedMSG(byte[] data, TYPE_MSG type) {
		switch (type)
		{
		case ERROR :
			break;
		case HELLO_SERVER : 
			receivedHelloServer(data);
			break;
		case PUSH_NEW_FILE :
			receivedPushNewFile(data);
			break;
		case DELETE :
			receivedDeleteFile(data);
			break;
		case ACK_HELLO_CLIENT :
			receivedACKHelloClient(data);
			break;
		case ACK_DOWNLOAD :
			receivedDownloadServer(data);
			break;
		default :

		}
	}

	/**
	 * Callback used when the server respond to a Hello_Client Notification
	 * @param data
	 */
	private void receivedACKHelloClient(byte[] data) {
		// Send first document to server (don't need to use the cache)	
		System.err.println("ACK HELLO");
		//Handler Interface 
	}

	/**
	 * Callback used when the server boot
	 * @param data
	 */
	private void receivedHelloServer(byte[] data) {
		// TODO Auto-generated method stub

	}

	/**
	 * Callback used when the server send a new document information
	 * @param data
	 */
	private void receivedPushNewFile(byte[] data) {
		System.out.println("NEW FILE HAS BEEN PUSHED !");
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int versionClient = tmp.getInt(0);

		int urlSize = tmp.getInt(4);
		byte[] urlb = new byte[urlSize];
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);

		System.out.println("New Document : "+url +"( Version : "+versionClient+")");
		urls.add(url);
		System.out.println("Nombre de documents : " +urls.size());	
		ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
		docTab.putInt(url.length());
		docTab.put(url.getBytes());
		nio.send(docTab.array(),TYPE_MSG.DOWNLOAD);

	}

	/**
	 * Callback used when the server send a Delete Notification
	 * @param data
	 */
	private void receivedDeleteFile(byte[] data) {
		System.out.println("FILE HAS BEEN DELETED !");
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int versionClient = tmp.getInt(0);
		int urlSize = tmp.getInt(4);

		byte[] urlb = new byte[urlSize];
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);

		String url = new String(urlb);
		System.out.println("File Name: "+ url);

		urls.remove(url);
		System.out.println("Nombre de documents disponibles : " +urls.size());

	}

	/**
	 * Callback used when the server respond to a Download Notification
	 * @param data
	 */
	private void receivedDownloadServer(byte[] data) {
		I_Document doc = bytesToI_Document(data);
		System.out.println("Test Download : "+ doc.getOwner() +"-"+doc.getUrl()+"-"+doc.getVersionNumber() );			
		//TEST : Modify and upload
		doc.setVersionNumber((doc.getVersionNumber()+1));
		byte[] docU = I_DocumentToByte(doc);
		nio.send(docU, TYPE_MSG.UPLOAD);
	}


	
	public void addFile(I_Document doc)
	{
		byte[] docUpload = I_DocumentToByte(doc);
		nio.send(docUpload,TYPE_MSG.UPLOAD);
	}
	
	/**
	 * Not used 
	 * @param urlD
	 */
	public void deleteFile(String urlD)
	{
		ByteBuffer docTab = ByteBuffer.allocate(urlD.length() + 4);
		docTab.putInt(urlD.length());
		docTab.put(urlD.getBytes());
		nio.send(docTab.array(),TYPE_MSG.DELETE);
	}
	
	/**
	 * @param cache the cache to set
	 */
	public void setCache(List<I_Document> cache) {
		//this.cache = cache;
	}
	
	public void addCache(I_Document doc){
		//cache.add(doc);
	}
	
	public void removeCache(I_Document doc){
		//cache.remove(doc);
	}

	/**
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
