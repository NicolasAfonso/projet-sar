//package engine;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.nio.channels.SocketChannel;
//import java.util.Scanner;
//
//public class Test implements I_RecvMsgHandler{
//	I_NioEngine nioEngine;
//	String name;
//	int compteur = 0;
//	boolean client = false;
//	public Test(String[] args) throws IOException {
//
//		if (args.length > 0 && args[0].equalsIgnoreCase("-s")){
//			System.out.println("Starting nio server");
//			name="Server";
//			InetAddress addr = InetAddress.getByName("localhost");  
//			nioEngine = new NioEngine();
//			nioEngine.initializeAsServer(addr, 8888, this);
//			nioEngine.mainloop();
//
//		} else {
//			client = true ;
//			System.out.println("Starting nio client");
//			name="Client";
//			InetAddress addr = InetAddress.getByName("localhost");  
//			Scanner sc = new Scanner(System.in);
//			System.out.println("Enter your message :");
//			String message = sc.nextLine();
//			nioEngine = new NioEngine();
//			nioEngine.initializeAsClient(addr, 8888, this);
//			nioEngine.send(message.getBytes(),TYPE_MSG.MESSAGE);
//			nioEngine.mainloop();
//
//		}
//	}
//
//	public void receivedCB(byte[] data, SocketChannel socketChannel){
//		if(client)
//		{
//			System.out.println(name + " received: " + new String(data));
////			Scanner sc = new Scanner(System.in);
////			System.out.println("Veuillez saisir un message :");
////			String message = sc.nextLine();
////			nioEngine.send(message.getBytes(),TYPE_MSG.MESSAGE);
//			nioEngine.send(data,TYPE_MSG.MESSAGE);
//		}
//		else
//		{
//			System.out.println(name + " received: " + new String(data));;
////			nioEngine.send(socketChannel,"ACK".getBytes(),TYPE_MSG.ACK);
//			nioEngine.send(socketChannel,data,TYPE_MSG.ACK);
//		}
//	}
//
//
//	public static void main(String[] args) {
//		try {
//			new Test(args);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//}
