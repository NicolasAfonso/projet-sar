package engine;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

import server.I_ServerHandler;
import client.I_ClientHandler;

public interface I_NioEngine {
	
	/**
	 * Initialize the engine with the server configuration
	 * @param hostAddress
	 * @param port
	 * @param handler
	 * @throws IOException
	 */
	public void initializeAsServer(InetAddress hostAddress,int port, I_ServerHandler handler) throws IOException;
	
	/**
	 * Initialize the engine with the client configuration
	 * @param hostAddress
	 * @param port
	 * @param handler
	 * @throws IOException
	 */
	public void initializeAsClient(InetAddress hostAddress,int port, I_ClientHandler handler) throws IOException;
	
	/**
	 * Nio Key selector
	 */
	public void mainloop();
	
	/**
	 * Send data with only Data and Message Type (Use by Client)
	 * @param data
	 * @param type
	 */
	public void send(byte[] data,TYPE_MSG type);
	
	/**
	 * Send dant with the socketChannel, Data and Message Type (Use by Server) 
	 * @param socketChannel
	 * @param data
	 * @param type
	 */
	public void send (SocketChannel socketChannel, byte[] data,TYPE_MSG type);
	
	/**
	 * Closed the connexion
	 */
	public void terminate();
	
}
