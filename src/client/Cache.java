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
import org.apache.log4j.Logger;
import document.I_Document;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;

public class Cache implements I_CacheHandler{

	private int id ; 			// cache id
	private List<String> urls ;	// an url is a unique document identifier. urls contains the list of the documents available on the cache.
	private I_NioEngine nio ;	// the nioEngine
	private ByteBuffer tmp ;
	private String[] args;		// client id and port
	private Thread nioT ;
	private static final Logger logger = Logger.getLogger(Cache.class);
	private I_APICache handlerAPI;
	private I_Document tmpD ;
	private String serverName ;
	private int serverPort;
	private InetAddress addrServer;

	/**
	 * Cache constructor
	 * @param args the arguments passed by the client: id and port 
	 * @param hc
	 */
	public Cache(String[] arg,I_APICache hc)
	{
		id = Integer.parseInt(arg[0]) ;
		nio = new NioEngine();
		urls =  new ArrayList<String>();
		args = arg;
		nioT = new Thread(nio);
		handlerAPI = hc ;
		serverName = "localhost";
		serverPort = Integer.parseInt(arg[1]);
		try {
			addrServer = InetAddress.getByName(serverName);
		} catch (UnknownHostException e) {
			logger.error(" Unknown Host");
			System.exit(0);
		}

	}

	/**
	 * Cache initialization, client connection.
	 */
	public void init(){
		if(serverPort < 1024)
		{
			logger.error("The use of port number below 1024 is forbidden, they are reserved for your system.");
			System.exit(0);
		}
		try {
			// Connection			
			nio.initializeAsClient(addrServer,serverPort,this);

			// send client id to server
			tmp = ByteBuffer.allocate(4);
			tmp.putInt(id);
			nio.send(tmp.array(), TYPE_MSG.HELLO_CLIENT);
			nioT.start();

		} catch (IOException e) {
			logger.error("Initialize Client error ");
			System.exit(0);
		} 
	}

	@Override
	public void receivedMSG(byte[] data, TYPE_MSG type) {
		switch (type)
		{
		case ERROR :
			receivedError(data);
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
		case ACK_LOCK : 
			reveivedACKLockServer(data);
			break;
		case ACK_UNLOCK : 
			reveivedACKUnlockServer(data);
			break;
		case ACK_DOWNLOAD :
			receivedDownloadServer(data);
			break;
		case ACK_UPLOAD :
			receivedUploadServer(data);
			break;
		case ACK_LIST_FILE :
			reveivedListServer(data);
			break;
		default :
			receivedError(data);
		}
	}

	
/*	MESSAGE HANDLERS
*/
	
	
	/**
	 * Handler called when an error message is received.
	 * @param data
	 */
		private void receivedError(byte[] data) {

			//int prefixError = Integer.getInteger(new String(data));
			tmp = ByteBuffer.allocate(data.length);
			tmp.put(data);
			tmp.rewind();
			int prefixError = tmp.getInt(0);
			//String prefixError = new String(data);
			logger.warn("Error detected. Type : " + prefixError);

			// we will use error prefixes
			if (prefixError == 0)	
				nio.terminate();

			handlerAPI.handlerError(prefixError);

		}
	
	/**
	 * 
	 * @param data
	 */
		private void reveivedACKLockServer(byte[] data) {
			logger.info("Received Lock on "+ new String(data));
			handlerAPI.handlerLockFile();


		}
	
	/**
	 * 
	 * @param data
	 */
	private void reveivedACKUnlockServer(byte[] data) {
		logger.info("File unlocked : "+new String(data));
		handlerAPI.handlerUnlockFile();

	}
	
/**
 * 
 * @param data
 */
	private void receivedUploadServer(byte[] data) {
		logger.info("File updated : "+new String(data));
		handlerAPI.handlerUpdateFile();

	}
	

	

	
/**
 * 
 * @param data
 */
	private void reveivedListServer(byte[] data) {
		logger.info("Received ACK LIST");
		if(!urls.isEmpty())
		{
			urls.clear();
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			List<String> docs = (List<String>) in.readObject(); 
			bis.close();
			in.close();

			for(String s : docs)
			{
				urls.add(s);
			}

		} catch (IOException e) {
			logger.error(" Error to received the list");
		} catch (ClassNotFoundException e) {
			logger.error(" Error to received the list");
		}
		handlerAPI.handlerListFile(urls);
	}

	/**
	 * Callback used when the server respond to a Hello_Client Notification
	 * @param data
	 */
	private void receivedACKHelloClient(byte[] data) {
		// Send first document to server (don't need to use the cache)	

		//Handler Interface 
		nio.send("".getBytes(),TYPE_MSG.LIST_FILE);

	}

	/**
	 * Callback used when the server boot
	 * @param data
	 */
	private void receivedHelloServer(byte[] data) {
		logger.info("Received HELLO_SERVER");

	}

	/**
	 * Callback used when the server send a new document information
	 * @param data
	 */
	private void receivedPushNewFile(byte[] data) {
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int versionClient = tmp.getInt(0);

		int urlSize = tmp.getInt(4);
		byte[] urlb = new byte[urlSize];
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);
		logger.info("Received PUSH_NEW_FILE :"+ url +"-Version  "+versionClient);
		urls.add(url);
		handlerAPI.handlerPushNewFile(url);
	}

	/**
	 * Callback used when the server send a Delete Notification
	 * @param data
	 */
	private void receivedDeleteFile(byte[] data) {
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int versionClient = tmp.getInt(0);
		int urlSize = tmp.getInt(4);
		byte[] urlb = new byte[urlSize];
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);
		urls.remove(url);
		logger.info("Received DELETE_FILE :"+ url +"-Version  "+versionClient);
		handlerAPI.handlerDeleteFile();
	}

	/**
	 * Callback used when the server respond to a Download Notification
	 * @param data
	 */
	private void receivedDownloadServer(byte[] data) {
		I_Document doc = bytesToI_Document(data);
		tmpD = doc ;
		logger.info("Received ACK_DOWNLOAD :"+ doc.getUrl() +"-Version  "+doc.getVersionNumber());
		handlerAPI.handlerReceivedFile();

	}


/**
 * 
 * @param doc
 */
	public void addFile(I_Document doc)
	{
		if(!urls.contains(doc.getUrl()))
		{
			byte[] docUpload = I_DocumentToByte(doc);
			nio.send(docUpload,TYPE_MSG.UPLOAD);
		}

	}

	/**
	 * 
	 * @param urlD
	 */
	public void deleteFile(String urlD)
	{
		if(urls.contains(urlD))
		{
			ByteBuffer docTab = ByteBuffer.allocate(urlD.length() + 4);
			docTab.putInt(urlD.length());
			docTab.put(urlD.getBytes());
			nio.send(docTab.array(),TYPE_MSG.DELETE);
		}
	}
	
/**
 * 
 */
	public void listFile()
	{					
		handlerAPI.handlerListFile(urls);
	}
	
/**
 * 
 * @param url
 */
	public void lockFile(String url){
		if(urls.contains(url))
		{
			ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
			docTab.putInt(url.length());
			docTab.put(url.getBytes());
			nio.send(docTab.array(),TYPE_MSG.LOCK);
		}

	}
	
/**
 * 
 */
	public void updateFile(){
		if(tmpD !=null)
		{
			tmpD.setVersionNumber(tmpD.getVersionNumber()+1);
			byte[] docU = I_DocumentToByte(tmpD);
			nio.send(docU, TYPE_MSG.UPLOAD);
			tmpD = null;
		}

	}

	/**
	 * 
	 * @param message
	 */
	public void downloadFile(String message) {
		tmp = ByteBuffer.allocate(message.length()+4);
		tmp.putInt(message.length());
		tmp.put(message.getBytes());
		nio.send(tmp.array(),TYPE_MSG.DOWNLOAD);
	}
	
	/**
	 * 
	 * @param url
	 */
	public void unlockFile(String url)
	{
		ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
		docTab.putInt(url.length());
		docTab.put(url.getBytes());
		nio.send(docTab.array(),TYPE_MSG.UNLOCK);


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
			logger.error(" Error convert Bytes in I_Document");
		} catch (ClassNotFoundException e) {
			logger.error(" Error convert Bytes in I_Document");
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
			logger.error(" Error convert I_Document in Bytes");
		}
		return null;
	}

	@Override
	public void serverNotAvailable() {
		logger.info("Server isn't available");
		boolean serverReboot = false;
		while(!serverReboot)
		{			
			try {
				nio.reconnect(addrServer, serverPort);
				nio.initializeAsClient(addrServer, serverPort, this);
				serverReboot = nio.isConnected();
			} catch (IOException e) {

			} catch (InterruptedException e) {

			}
		}
		logger.info("Server available");
		handlerAPI.handlerServerAvailable();
		tmp = ByteBuffer.allocate(4);
		tmp.putInt(id);
		nio.send(tmp.array(), TYPE_MSG.HELLO_CLIENT);
		nio.mainloop();
	}

	public I_Document getCurrentFile() {
		return tmpD;

	}



}
