
package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import document.Document;
import document.I_Document;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;

public class A_Client implements I_ClientHandler{

	private int id ; 
	private Cache cache ;
	private List<String> urls ;
	private I_NioEngine nio ;
	ByteBuffer tmp ;

	public A_Client(String[] args)
	{
		id = Integer.parseInt(args[0]) ;
		nio = new NioEngine();
		urls =  new ArrayList<String>();
		cache = new Cache();

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

	public static void main(String[] args) {
		try {
			new A_Client(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivedMSG(byte[] data, TYPE_MSG type) {
		
		if (type == TYPE_MSG.PUSH_NEW_FILE){
			System.out.println("NEW FILE HAS BEEN PUSHED !");
		
		nio.send(new String("Ack").getBytes(),TYPE_MSG.ACK);}
		else {

			System.out.println(type);


			// Send first document to server (don't need to use the cache)
			Document doc = new Document("tutus",id);
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
			//nio.send(data, TYPE_MSG.HELLO_CLIENT);
		}
	}

}
