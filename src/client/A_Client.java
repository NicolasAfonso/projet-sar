
package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

import document.Document;
import document.I_Document;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;

public class A_Client implements I_ClientHandler,Runnable{

	private int id ; 
	private Cache cache ;
	private List<String> urls ;
	private I_NioEngine nio ;
	ByteBuffer tmp ;
	private String[] args;

	@Override
	public void run() {


	}
	public A_Client(String[] a)
	{
		id = Integer.parseInt(a[0]) ;
		nio = new NioEngine();
		urls =  new ArrayList<String>();
		cache = new Cache();
		args = a;
		init(args);

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


			nio.mainloop();


		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
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
		Document doc = new Document("tutus"+id,id);
		byte[] file = new String("Je suis un fichier").getBytes();
		doc.setFile(file);

		tmp = ByteBuffer.allocate(4+4+doc.getUrl().length()+4+file.length);
		//tmp.putInt(doc.getVersionNumber());
		tmp.putInt(9);

		tmp.putInt(doc.getUrl().length());

		tmp.put(doc.getUrl().getBytes());
		tmp.putInt(file.length);
		tmp.put(doc.getFile());
		System.out.println("Message : " +  new String(tmp.array()));

		nio.send(tmp.array(),TYPE_MSG.UPLOAD);
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
		System.out.println("VERSION CLIENT: "+ versionClient);
		int urlSize = tmp.getInt(4);
		System.out.println("TAILLE URL: "+ urlSize);
		byte[] urlb = new byte[urlSize];
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);

		System.out.println("TABLEAU URL: "+ new String(urlb));

		String url = new String(urlb);

		urls.add(url);
		System.out.println("Nombre de documents : " +urls.size());	
		ByteBuffer docTab = ByteBuffer.allocate(url.length() + 4);
		docTab.putInt(url.length());
		docTab.put(url.getBytes());
		nio.send(docTab.array(),TYPE_MSG.DOWNLOAD);

	}
	
	/**
     * Callback used when the server respond to a Download Notification
     * @param data
     */
	private void receivedDownloadServer(byte[] data) {
		//METHOD WITH ObjectInputStream -> Branch ObjectOutputStream on GIT. Search another solution 
//		ByteArrayInputStream bis = new ByteArrayInputStream(data);
//		ObjectInput in = null;
//
//		try {
//			in = new ObjectInputStream(bis);
//			I_Document o = (I_Document) in.readObject(); 
//			System.out.println("Test Download : "+ o.getOwner() +"-"+o.getUrl()+"-"+o.getVersionNumber() );
//			bis.close();
//			in.close();
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}


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

	public static void main(String[] args) {
		try {
			new A_Client(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
