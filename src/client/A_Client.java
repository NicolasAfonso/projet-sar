
package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
			addr = InetAddress.getByName("localhost");		
			nio.initializeAsClient(addr,Integer.parseInt(args[1]),this);
			tmp = ByteBuffer.allocate(13);
			tmp.put("t".getBytes());
			
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
		System.out.println(type);
		
		nio.send(data, TYPE_MSG.HELLO_CLIENT);
		
	}
	
}
