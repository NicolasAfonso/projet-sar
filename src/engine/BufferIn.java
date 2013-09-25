package engine;

import java.nio.ByteBuffer;

public class BufferIn {
	
 public ByteBuffer buffin ;
 public STATE state;
 public int message_size; 
 public int message_id;
 	public BufferIn() {
 		state = STATE.SIZE;
 		//Initialize bufferin to wait a Size
 		buffin = ByteBuffer.allocate(4);
 		message_size = 0;
	}
}
