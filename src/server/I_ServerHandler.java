package server;

import java.nio.channels.SocketChannel;

import engine.TYPE_MSG;

public interface I_ServerHandler {

	public void receivedMSG(byte[] data, TYPE_MSG type,SocketChannel socketChannel);
}
