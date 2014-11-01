package server;

import java.nio.channels.SocketChannel;

import engine.Client;
import engine.TYPE_MSG;

public interface I_ServerHandler {
	
	/**
	 * Server General Callback. Use by NIO layer to notify a new message
	 * @param data
	 * @param type
	 * @param socketChannel
	 */
	 
	public void receivedMSG(byte[] data, TYPE_MSG type,SocketChannel socketChannel);

	public void clientDisconnected(Client client);
}
