package server;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import client.A_Client;
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
	ByteBuffer tmp ;
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
			nio.mainloop();
			
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
			case UPLOAD :
				receivedUploadClient(data,socketChannel);
				break;
			default :
				
		}
		
		System.out.println(new String(data));
		//nio.send(socketChannel,data, TYPE_MSG.HELLO_SERVER);
		
		
	}

	private void receivedUploadClient(byte[] data, SocketChannel socketChannel) {
		//ByteBuffer dataBB = ByteBuffer.allocate(data.length);
		//Read version
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		
		//tmp.put(data, 0, 3);
		tmp.rewind();
		//tmp.put(dataBB) ;
		
		
		int versionClient = tmp.getInt(0);
		
		System.out.println("VERSION CLIENT: "+ versionClient);
		
		
		int urlSize = tmp.getInt(4);
		System.out.println("TAILLE URL: "+ urlSize);
		
		
		byte[] urlb = new byte[urlSize];//ok
		//tmp.get(urlb,7,5);
		tmp.position(8);
		tmp.get(urlb, 0, urlSize);
		
		System.out.println("TABLEAU URL: "+ new String(urlb));
		
		String url = new String(urlb);
		int fileSize = tmp.getInt(8+urlSize);
		System.out.println("TAILLE FILE: "+ fileSize);
		
		byte[] fileb = new byte[fileSize];
		tmp.position(8+urlSize+4);
		tmp.get(fileb, 0, fileSize);
		System.out.println("TABLEAU FILE: "+ new String(fileb));
		
		//String file = new String(tmp.get(fileb,urlSize,fileSize).array());
		//System.out.println("PLOP");
		//System.out.println("URL RECUP " + url + " doc" + new String(file)+ " version " + versionClient);
		
		//String url = null;
		I_Document doc = documents.get(url); // on suppose que c'est juste un objet pour l'instant
		if(doc == null)
		{
			doc = new Document(url, nio.getClient(socketChannel).getId()) ;
			documents.put(url,doc);
			docsClient.put(nio.getClient(socketChannel).getId(), doc);
			
			nio.pushDocument(doc, "New Doc");
		}
		else
		{
			if(doc.getVersionNumber() < versionClient)
			{
				//doc.setFile(file);
			}
		}
		
		
	}

	private void receivedHelloClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		tmp = ByteBuffer.allocate(4);
		tmp.put(data);
		tmp.rewind();
		int i = tmp.getInt();
		System.out.println("TUTU"+i);
		c.setId(i);
		nio.send(socketChannel, data, TYPE_MSG.ACK);
	}
	
}
