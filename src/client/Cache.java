
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
import java.util.Scanner;

import javax.swing.plaf.SliderUI;

import document.Document;
import document.I_Document;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;
import org.apache.log4j.Logger;
public class Cache implements I_CacheHandler{

	private int id ; 
	private List<String> urls ;
	private I_NioEngine nio ;
	private ByteBuffer tmp ;
	private String[] args;
	private Thread nioT ;
	private static final Logger logger = Logger.getLogger(Cache.class);
	private I_APICache handlerAPI;
	I_Document tmpD ;
	
	public Cache(String[] a,I_APICache hc)
	{
		id = Integer.parseInt(a[0]) ;
		nio = new NioEngine();
		urls =  new ArrayList<String>();
		args = a;;
		nioT = new Thread(nio);
		handlerAPI = hc ;
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
		case ACK_LIST_FILE :
			reveivedListServer(data);
		default :

		}
	}

	private void reveivedListServer(byte[] data) {
		logger.info("Received ACK LIST");
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
		// TODO Auto-generated method stub
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
	}

	/**
	 * Callback used when the server respond to a Download Notification
	 * @param data
	 */
	private void receivedDownloadServer(byte[] data) {
		I_Document doc = bytesToI_Document(data);
		tmpD = doc ;
		logger.info("Received ACK_DOWNLOAD :"+ doc.getUrl() +"-Version  "+doc.getVersionNumber());
		//TODO : Add to list current file		
		
	}


	
	public void addFile(I_Document doc)
	{
		if(!urls.contains(doc.getUrl()))
		{
			byte[] docUpload = I_DocumentToByte(doc);
			nio.send(docUpload,TYPE_MSG.UPLOAD);
			handlerAPI.handlerAddFile(true);
		}
		else
		{
			handlerAPI.handlerAddFile(false);
		}
	}
	
	/**
	 * Not used 
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
			handlerAPI.handlerDeleteFile(true);
		}
	}
	
	public void listFile()
	{					
		handlerAPI.handlerListFile(urls);
	}
	
	public void lockFile(String url){
		if(urls.contains(url))
		{
			ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
			docTab.putInt(url.length());
			docTab.put(url.getBytes());
			nio.send(docTab.array(),TYPE_MSG.DOWNLOAD);
			handlerAPI.handlerLockFile(true);
		}
		else
		{
			handlerAPI.handlerLockFile(false);
		}
	}
	
	public void updateFile(){
		byte[] docU = I_DocumentToByte(tmpD);
		nio.send(docU, TYPE_MSG.UPLOAD);
		handlerAPI.handlerUpdateFile(true);
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
