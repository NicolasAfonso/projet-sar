package engine;
import java.nio.channels.SocketChannel;


public interface I_RecvMsgHandler {

	/**
	 * Handler when received data
	 * @param data
	 * @param socketChannel
	 */
	public void receivedCB(byte[] data,SocketChannel socketChannel);
}
