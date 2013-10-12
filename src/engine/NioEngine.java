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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import server.I_ServerHandler;
import client.Cache;
import client.I_CacheHandler;
import document.I_Document;



public class NioEngine implements I_NioEngine{

	//Generic 
	private InetAddress address ;
	private int port ;
	private Selector selector ;
	private I_CacheHandler handlerClient;
	private I_ServerHandler handlerServer;
	private Map<SocketChannel, Client> clients = new HashMap<>();
	//private Map<Client,SocketChannel> socketChannels = new HashMap<>();
	private Map<Integer,Client> clientsID = new HashMap<>();
	private static final Logger logger = Logger.getLogger(NioEngine.class);
	//Server info
	private ServerSocketChannel serverChannel ;

	//Client info 
	private SocketChannel socketChannel_Client  ;
	public NioEngine(){
		//Server info
		serverChannel=null ;
		socketChannel_Client =null ;
	}

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
			//logger.info("Server created on : "+port+"    [OK]");
		}catch(Exception e){
			//TODO
			logger.error("Initialize server error " + e.toString());
		}
	}
	
	@Override
	public void initializeAsClient(InetAddress hostAddress, int port,I_CacheHandler handler){
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
			clients.put(socketChannel_Client, new Client(socketChannel_Client));
			//Listen client's channel
			socketChannel_Client.register(selector, SelectionKey.OP_CONNECT);
			//logger.info("Created client");

		}catch(Exception e){
			logger.error("Initialize client error :",e);
			try {
				socketChannel_Client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
				logger.error("Main loop error " ,e);}
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
			logger.info("Client connected : "+socket.toString());

			//Associate the socket and Client
			Client c = new Client(socketChannel) ;
			clients.put(socketChannel,c);

			
			// Wait a READ event. 
			socketChannel.register(this.selector, SelectionKey.OP_READ);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Erreur Accept : ", e);
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
			logger.error("Connexion client error :",e);
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
		Client c = clients.get(socketChannel);
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
					clients.remove(socketChannel);
					socketChannel.close();
					if(socketChannel_Client!=null)
					{
						socketChannel_Client.close();
						handlerClient.serverNotAvailable();
					}
					else
					{	
						handlerServer.clientDisconnected(c);
						logger.warn("Client "+c.getId() +" is disconnected");
					}

					//System.out.println("STYPE_MSG type = null;IZE numRead = -1 , delete key and close channel");
					return;
				}

				if(in.buffin.remaining()==0){

					in.state = STATE.TYPE;
					in.message_size = in.buffin.getInt(0);
					//System.out.println( "Message Size : " + in.message_size);
					in.buffin = ByteBuffer.allocate(4);
				}

			}

			if(in.state == STATE.TYPE)
			{
				numRead = socketChannel.read(in.buffin);
				if(numRead==-1)
				{
					key.cancel();
					clients.remove(socketChannel);
					socketChannel.close();
					logger.warn("Client deleted TYPE ERROR");
					//System.out.println(" Type numRead = -1 , delete key and close channel");
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
						clients.remove(socketChannel);
						socketChannel.close();
						logger.warn("Client deleted TYPE ERROR");
						//System.err.println(" Data numRead = -1 , delete key and close channel");
						return;
					}
				}		

				if(in.buffin.remaining() == 0)
				{	
					//System.out.println("Received " + (in.buffin.capacity()+4+4) +" bytes (Message size : " + in.buffin.capacity()+")" );
					//logger.info("Received "+type.toString()+" to "+clients.get(socketChannel).getId());
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
			logger.error("Reading Error : " , e);
			//En cas d'erreur , on reinitialise le canal
			try {
				socketChannel.close();
			} catch (IOException e1) {
				//TODO
				logger.error("Error socket close",e1);
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

	//	System.out.println("Send "+ out.capacity() + " bytes ( Message size : " + (out.capacity()-4)  +" bytes)");
		try{
			socketChannel.write(out);
			socketChannel.register(this.selector, SelectionKey.OP_READ);

		}catch(IOException e)
		{
			key.cancel();
			socketChannel.close();
			logger.error("Sending Error :",e);
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
			logger.error("Error send",e);
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
		//logger.info("Send "+type.toString()+" to "+clients.get(socketChannel).getId());
		selector.wakeup();
	}

	@Override
	public void terminate() {
		try {

			if(socketChannel_Client != null)
			{
				socketChannel_Client.close();
				socketChannel_Client.keyFor(selector).cancel();
				
			}
			else
			{
				serverChannel.close();
				serverChannel.keyFor(selector).cancel();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Error in terminate",e);
		}

	}

	@Override
	public Client getClient(SocketChannel socketChannel) {
		
		return clients.get(socketChannel);
	}
	
	@Override
	public List<Client> getClients(){
		Object[] objs= clients.values().toArray();
		List<Client> clients = new ArrayList<Client>();
		for(Object o : objs)
		{
			clients.add(((Client)o));
		}
		return clients;
	}
	
	public void push(I_Document doc, TYPE_MSG type){
		
		String url = doc.getUrl();
		int version = doc.getVersionNumber();
		//String msg = version + url.length() + url ;
		
		//byte[] docTab =  new byte[url.length()+8];
		
		 //byte[] docTab = new byte(msg);
		ByteBuffer docTab = ByteBuffer.allocate(url.length() + 8);
		docTab.putInt(version);
		docTab.putInt(url.length());
		docTab.put(url.getBytes());
		
		Set<SocketChannel> keys = clients.keySet();
		Iterator it = keys.iterator();
		while (it.hasNext()){	
			send((SocketChannel) it.next(), docTab.array(), type);
		}
	}

	@Override
	public void run() {
		this.mainloop();
		
	}

	@Override
	public void reconnect(InetAddress hostAddress,int port) throws InterruptedException {
		if(socketChannel_Client!=null)	
		try {
				socketChannel_Client.close();
				selector.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


	}

	@Override
	public boolean isConnected() {
		if(socketChannel_Client !=null)
		{
			try {
				socketChannel_Client.finishConnect();			
				return socketChannel_Client.isConnected();
			} catch (IOException e) {
				return false;
			}

		}
		return false;
	}


}
