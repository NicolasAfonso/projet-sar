package client;

import engine.TYPE_MSG;

public interface I_ClientHandler {

	public void receivedMSG(byte[] array, TYPE_MSG type);
}
