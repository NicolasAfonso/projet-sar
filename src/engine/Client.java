package engine;

public class Client {
	
	private BufferIn buffin;
	private BufferOut buffout;
	
	public Client(){
		setBuffin(new BufferIn());
		setBuffout(new BufferOut());
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
}
