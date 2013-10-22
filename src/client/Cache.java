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
	private Thread nioT ;
	private static final Logger logger = Logger.getLogger(Cache.class);
	private I_APICache handlerAPI;
	private I_Document tmpD ;
	private String serverName ;
	private int serverPort;
	private InetAddress addrServer;
	private boolean serverfail;
	private String lockedFile;

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
		nioT = new Thread(nio);
		handlerAPI = hc ;
		serverName = "localhost";
		serverPort = Integer.parseInt(arg[1]);
		serverfail = false;
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
	 * @param data, a byte array containing an integer
	 */
	private void receivedError(byte[] data) {
		tmp = ByteBuffer.allocate(data.length);	// retrieving the integer
		tmp.put(data);
		tmp.rewind();
		int prefixError = tmp.getInt(0);
		logger.warn("Error detected. Type : " + prefixError);
		// we will use error prefixes - please refer you to the documentation for correspondence error/prefix
		if (prefixError == 0)	
			nio.terminate();
		handlerAPI.handlerError(prefixError);
	}


	/**
	 * Handler called when a lock ack message is received.
	 * @param data, a byte array containing the url of the locked document
	 */
	private void reveivedACKLockServer(byte[] data) {
		String url = new String(data);
		logger.info("Received Lock on "+ url);
		handlerAPI.handlerLockFile(url);
	}


	/**
	 * Handler called when an unlock ack message is received.
	 * @param data, a byte array containing the url of the unlocked document
	 */
	private void reveivedACKUnlockServer(byte[] data) {
		String url = new String(data);
		logger.info("File unlocked : "+url);
		handlerAPI.handlerUnlockFile(url);
	}


	/**
	 * Handler called when an upload ack message is received.
	 * @param data, a byte array containing the url of the uploaded document
	 */
	private void receivedUploadServer(byte[] data) {
		String url = new String(data);
		logger.info("File updated : "+url);
		handlerAPI.handlerUpdateFile(url);
	}


	/**
	 * Handler called when a list file ack message is received.
	 * @param data, a byte array containing a list of url files available on server.
	 */
	private void reveivedListServer(byte[] data) {
		logger.info("Received ACK LIST");
		if(!urls.isEmpty())	//we need to update the local url file list
		{
			urls.clear();
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			@SuppressWarnings("unchecked") //we are sure the object received is a list of url
			List<String> docs = (List<String>) in.readObject(); 
			bis.close();
			in.close();

			for(String s : docs)
			{
				urls.add(s);	// file list updated
			}

		} catch (IOException e) {
			logger.error("Error while updating the local file list available from server.");
		} catch (ClassNotFoundException e) {
			logger.error("Error while updating the local file list available from server.");
		}
		handlerAPI.handlerListFile(urls);
	}


	/**
	 * Handler called when a helloClient ack message is received.
	 * @param data, empty array
	 */
	private void receivedACKHelloClient(byte[] data) {
		nio.send("".getBytes(),TYPE_MSG.LIST_FILE);
	}


	/**
	 * Callback used when the server boot. Client is notified that server is available.
	 * @param data
	 */
	private void receivedHelloServer(byte[] data) {
		logger.info("Received HELLO_SERVER");
	}


	/**
	 * Callback used when the server send a new document information
	 * @param data, a byte array containing the version and url of the new file
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
		urls.add(url); // need to update the local url list
		handlerAPI.handlerPushNewFile(url);
	}


	/**
	 * Callback used when the server send a Delete Notification
	 * @param data, a byte array containing the version and url of the deleted file
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

		logger.info("Received DELETE_FILE :"+ url +"-Version  "+versionClient);
		urls.remove(url); // need to update the local url list
		handlerAPI.handlerDeleteFile(url);
	}


	/**
	 * Callback used when the server respond to a Download Notification
	 * @param data, a byte array containing the previous requested document
	 */
	private void receivedDownloadServer(byte[] data) {
		I_Document doc = bytesToI_Document(data);
		tmpD = doc ;
		logger.info("Received ACK_DOWNLOAD :"+ doc.getUrl() +"-Version  "+doc.getVersionNumber());
		handlerAPI.handlerReceivedFile(doc.getUrl());
	}


	/**
	 * Method used when the client wants to add a file on the server
	 * @param doc, the document to add to server
	 */
	public void addFile(I_Document doc)
	{
		if (!serverfail){
			if(!urls.contains(doc.getUrl()))
			{
				byte[] docUpload = I_DocumentToByte(doc);
				nio.send(docUpload,TYPE_MSG.UPLOAD);
			}
			else
				handlerAPI.handlerError(5); // a file with the same name already exists in the system
		}
		else
			handlerAPI.handlerServerNotAvailable();
	}



	/**
	 * Method used when client wants to delete a file on the server
	 * @param urlD, the url of the document we want to delete
	 */
	public void deleteFile()
	{
		if (!serverfail){
			if(urls.contains(lockedFile))
			{
				ByteBuffer docTab = ByteBuffer.allocate(lockedFile.length() + 4);
				docTab.putInt(lockedFile.length());
				docTab.put(lockedFile.getBytes());
				nio.send(docTab.array(),TYPE_MSG.DELETE);
			}
			else
				handlerAPI.handlerError(3); // the file does not exists
		}else
			handlerAPI.handlerServerNotAvailable();
	}

	/**
	 * Method used for printing the local list file
	 */
	public void listFile()
	{		if (!serverfail) 			
		handlerAPI.handlerListFile(urls);
	else
		handlerAPI.handlerServerNotAvailable();
	}


	/**
	 * Method used for requesting a file lock
	 * @param url, the url of the file we want to lock
	 */
	public void lockFile(String url){
		if (!serverfail) {

			if(urls.contains(url))
			{
				lockedFile = url;
				ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
				docTab.putInt(url.length());
				docTab.put(url.getBytes());
				nio.send(docTab.array(),TYPE_MSG.LOCK);
			}
			else
				handlerAPI.handlerError(3); // the file does not exists
		}
		else
			handlerAPI.handlerServerNotAvailable();
	}


	/**
	 * Method used for requesting a file unlock
	 * @param url, the url of the file we want to unlock
	 */
	public void unlockFile()
	{if (!serverfail) {
		ByteBuffer docTab = ByteBuffer.allocate(lockedFile.length() + 4);
		docTab.putInt(lockedFile.length());
		docTab.put(lockedFile.getBytes());
		lockedFile = null;
		nio.send(docTab.array(),TYPE_MSG.UNLOCK);
	}else
		handlerAPI.handlerServerNotAvailable();

	}


	/**
	 * Method used for updating a file
	 */
	public void updateFile(){
		if (!serverfail) {
			if(tmpD !=null)
			{
				tmpD.setVersionNumber(tmpD.getVersionNumber()+1);
				byte[] docU = I_DocumentToByte(tmpD);
				nio.send(docU, TYPE_MSG.UPLOAD);
				tmpD = null;
			}
			else
				handlerAPI.handlerError(7); // the file does not exists
		}
		else
			handlerAPI.handlerServerNotAvailable();
	}


	public void openfile() {
		if(tmpD !=null){	// a doc is on the cache
			handlerAPI.handlerOpenFile(tmpD);
		}
		else
			handlerAPI.handlerError(7); // the file does not exists
	}

	/**
	 * 
	 * @param message
	 */
	public void downloadFile() {
		if (!serverfail) {
			//String message = lockedFile;
			tmp = ByteBuffer.allocate(lockedFile.length()+4);
			tmp.putInt(lockedFile.length());
			tmp.put(lockedFile.getBytes());
			nio.send(tmp.array(),TYPE_MSG.DOWNLOAD);
		}
		else 
			handlerAPI.handlerServerNotAvailable();
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
		serverfail = true;
		logger.info("Server is not available.");
		handlerAPI.handlerServerNotAvailable();
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
		serverfail = false;
		handlerAPI.handlerServerAvailable();
		tmp = ByteBuffer.allocate(4);
		tmp.putInt(id);
		nio.send(tmp.array(), TYPE_MSG.HELLO_CLIENT);
		nio.mainloop();
	}

	/**
	 * Return the current file
	 * @return
	 */
	public I_Document getCurrentFile() {
		return tmpD;

	}



}
