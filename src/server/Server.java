package server;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private HashMap<Client, Document> docsClient ;
	private I_NioEngine nio ;
	
	public Server(String[] args)
	{
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
		System.out.println(type);
		
		System.out.println(new String(data));
		nio.send(socketChannel,data, TYPE_MSG.HELLO_SERVER);
		
		
	}
}
