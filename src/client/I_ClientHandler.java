package client;

import engine.TYPE_MSG;

public interface I_ClientHandler {
	
	/**
	 * Client General Callback. Use by NIO layer to notify a new message
	 * @param array
	 * @param type
	 */
	public void receivedMSG(byte[] array, TYPE_MSG type);
}
