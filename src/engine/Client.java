package engine;

import java.io.Serializable;
import java.nio.channels.SocketChannel;

public class Client implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6048411205623655122L;
	private int id;
	private BufferIn buffin;
	private BufferOut buffout;
	private SocketChannel socketChannel;
	/**
	 * @return the socketChannel
	 */
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	/**
	 * @param socketChannel the socketChannel to set
	 */
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public Client(SocketChannel s){
		setBuffin(new BufferIn());
		setBuffout(new BufferOut());
		socketChannel = s ;
	}

	/**
	 * @return the buffin
	 */
	public BufferIn getBuffin() {
		return buffin;
	}

	/**
	 * @param buffin the buffin to set
	 */
	public void setBuffin(BufferIn buffin) {
		this.buffin = buffin;
	}

	/**
	 * @return the buffout
	 */
	public BufferOut getBuffout() {
		return buffout;
	}

	/**
	 * @param buffout the buffout to set
	 */
	public void setBuffout(BufferOut buffout) {
		this.buffout = buffout;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
}
