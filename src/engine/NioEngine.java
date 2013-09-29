package engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import server.I_ServerHandler;
import client.I_ClientHandler;



public class NioEngine implements I_NioEngine{

	//Generic 
	private InetAddress address ;
	private int port ;
	private Selector selector ;
	private I_ClientHandler handlerClient;
	private I_ServerHandler handlerServer;
	private Map<SocketChannel, Client> clients = new HashMap<>();

	//Server info
	private ServerSocketChannel serverChannel ;

	//Client info 
	private SocketChannel socketChannel_Client  ;

	/**
	 * Initialize the engine with the server configuration
	 * @param InetAddress hostAddress
	 * @param int port
	 * @param I_RecvMsgHandler handler
	 */
	@Override
	public void initializeAsServer(InetAddress hostAddress, int port,I_ServerHandler handler){
		try{
			this.address = hostAddress ;
			this.port = port ;
			this.selector = SelectorProvider.provider().openSelector();
			this.handlerServer = handler; 
			
			// Server Socket Channel creation (not blocking) 
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(address,port));

			//Socket Listen on port and wait new client
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Server created on : "+port+"    [OK]");
		}catch(Exception e){
			//TODO
			System.err.println("Initialize server error " + e.toString());
		}
	}
	
	@Override
	public void initializeAsClient(InetAddress hostAddress, int port,I_ClientHandler handler){
		try{
			this.address = hostAddress ;
			this.port = port ;
			this.handlerClient = handler; 

			// Client Socket Channel creation (not blocking) 
			socketChannel_Client = SocketChannel.open();
			socketChannel_Client.configureBlocking(false);
			socketChannel_Client.socket().setTcpNoDelay(true);
			socketChannel_Client.connect(new InetSocketAddress(hostAddress, this.port));
			selector = SelectorProvider.provider().openSelector();
			clients.put(socketChannel_Client, new Client());
			//Listen client's channel
			socketChannel_Client.register(selector, SelectionKey.OP_CONNECT);
			System.out.println("Created client [OK]");

		}catch(Exception e){
			System.err.println("Initialize client error :" + e.toString());
		}
	}

	@Override
	public void mainloop() {
		while(true){
			try{
				this.selector.select();
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while(selectedKeys.hasNext())
				{
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if(!key.isValid()){
						continue;
					}else if(key.isAcceptable()){
						handleAccept(key);
					}else if(key.isConnectable()){
						handleConnectable(key);
					}else if(key.isReadable())
					{
						handleDataIn(key);
					}else if(key.isWritable())
						handleDataOut(key);					
				}
			}	
			catch(Exception e){
				//TODO
				System.err.println("Main loop error " + e.toString());}
		}
	}

	/**
	 * Accept the client on ServerSocketChannel
	 * @param key
	 */
	private void handleAccept(SelectionKey key) {

		// Accept the new client and create a new socket to communicate with the client.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		try {
			SocketChannel socketChannel = serverSocketChannel.accept();
			Socket socket = socketChannel.socket();
			socketChannel.configureBlocking(false);
			socketChannel.socket().setTcpNoDelay(true);
			System.out.println("Client connected : "+socket.toString());

			//Associate the socket and Client
			clients.put(socketChannel,new Client());
			// Wait a READ event. 
			socketChannel.register(this.selector, SelectionKey.OP_READ);

			System.out.println("Wait client data");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Erreur Accept : "+ e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Validation connection by the client 
	 * @param key
	 */
	private void handleConnectable(SelectionKey key){
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try{
			if (socketChannel.finishConnect()) {
				System.out.println("Connecté : " + socketChannel.socket().toString());
			} else {
				System.out.println("Echec");
			}
		}catch(Exception e){
			System.err.println("Connexion client error :" + e.toString());
		}

	}

	/**
	 * Handler execute when READ_OP is selected
	 * Message content : SIZE(4)+TYPE(4)+DATA(X)
	 * @param key
	 */
	private void handleDataIn(SelectionKey key)  {

		SocketChannel socketChannel = (SocketChannel) key.channel();
		BufferIn in ;
		in = clients.get(socketChannel).getBuffin();
		TYPE_MSG type = null;
		int numRead;
		try{

			if(in.state == STATE.SIZE){
				in.buffin = ByteBuffer.allocate(4);
				numRead = socketChannel.read(in.buffin);
				//System.out.println( "[Size] Byte lu : "+numRead);
				if(numRead==-1)
				{
					key.cancel();
					socketChannel.close();
					System.out.println("S				TYPE_MSG type = null;IZE numRead = -1 , delete key and close channel");
					return;
				}

				if(in.buffin.remaining()==0){

					in.state = STATE.TYPE;
					in.message_size = in.buffin.getInt(0);
					System.out.println( "Message Size : " + in.message_size);
					in.buffin = ByteBuffer.allocate(4);
				}

			}

			if(in.state == STATE.TYPE)
			{
				numRead = socketChannel.read(in.buffin);
				if(numRead==-1)
				{
					key.cancel();
					socketChannel.close();
					System.out.println(" Type numRead = -1 , delete key and close channel");
					return;
				}
				if(in.buffin.remaining()==0){
					type = TYPE_MSG.valueOf(in.buffin.getInt(0));
					in.buffin = ByteBuffer.allocate(in.message_size);
					in.state = STATE.DATA;
				}
			}

			if(in.state == STATE.DATA)
			{

				if(in.buffin.remaining()!=0)
				{
					numRead = socketChannel.read(in.buffin);
					//System.out.println("[DATA] Byte lu : " +numRead);
					if(numRead==-1)
					{
						key.cancel();
						socketChannel.close();
						System.err.println(" Data numRead = -1 , delete key and close channel");
						return;
					}
				}		

				if(in.buffin.remaining() == 0)
				{	
					System.out.println("Received " + (in.buffin.capacity()+4+4) +" bytes (Message size : " + in.buffin.capacity()+")" );
					//Le buffer est plein , on a donc reçu le message que l'on attendait, car celui-ci était de la taille du message. On peut donc executer les actions. 
					in.state = STATE.SIZE;
					if(socketChannel_Client != null)
					{
						handlerClient.receivedMSG(in.buffin.array(),type);
					}else
					{
						handlerServer.receivedMSG(in.buffin.array(),type,socketChannel);
					}
				}
			}

		}catch(Exception e){
			System.err.println("Reading Error : " + e.toString());
			//En cas d'erreur , on reinitialise le canal
			try {
				socketChannel.close();
			} catch (IOException e1) {
				//TODO
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Handler execute when WRITE_OP is selected
	 * @param key
	 * @throws IOException
	 */
	private void handleDataOut(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();


		ByteBuffer out ;
		out = clients.get(socketChannel).getBuffout().buffout;

		System.out.println("Send "+ out.capacity() + " bytes ( Message size : " + (out.capacity()-4)  +" bytes)");
		try{
			socketChannel.write(out);
			socketChannel.register(this.selector, SelectionKey.OP_READ);

		}catch(IOException e)
		{
			key.cancel();
			socketChannel.close();
			System.err.println("Sending Error :" +e.toString());
		}

	}

	@Override
	public void send(byte[] data,TYPE_MSG type) {

		try {
			if (socketChannel_Client.finishConnect()){
				send(socketChannel_Client,data,type);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void send(SocketChannel socketChannel, byte[] data,TYPE_MSG type) {
		ByteBuffer z = ByteBuffer.allocate(data.length+4+4); //Data + Message Size + Type of Message
		z.putInt(data.length);
		z.putInt(type.ordinal());
		z.put(data);
		//System.out.println("Taille du message à envoyer : z.getInt(0));
		clients.get(socketChannel).getBuffout().buffout = ByteBuffer.wrap(z.array());	

		SelectionKey key = socketChannel.keyFor(this.selector);
		key.interestOps(SelectionKey.OP_WRITE);
	}

	@Override
	public void terminate() {
		try {

			if(socketChannel_Client != null)
			{
				socketChannel_Client.close();
			}
			else
			{
				serverChannel.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
