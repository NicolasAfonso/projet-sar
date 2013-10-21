package client;

import java.util.List;


public interface I_APICache {
	
	/**
	 * Actions the cache have to perform when a delete file ack message is received. 
	 * @param url
	 */
	public void handlerDeleteFile(String url);
	
	/**
	 * Actions the cache have to perform when a file list ack message is received.
	 * @param urlsAvailable
	 */
	public void handlerListFile(List<String> urlsAvailable);
	
	/**
	 * Actions the cache have to perform when a lock file ack message is received.
	 * @param url
	 */
	public void handlerLockFile(String url) ;
	
	/**
	 * Actions the cache have to perform when an update file ack message is received.
	 * @param url
	 */
	public void handlerUpdateFile(String url);
	
	/**
	 * Actions the cache have to perform when the server is available.
	 */
	public void handlerServerAvailable();
	
	/**
	 * Actions the cache have to perform when the server is available.
	 */
	public void handlerServerNotAvailable();
	
	/**
	 * Actions the cache have to perform when a download ack message is received.
	 * @param url
	 */
	public void handlerReceivedFile(String url);
	
	/**
	 * Actions the cache have to perform when an unlock ack message is received.
	 * @param url
	 */
	public void handlerUnlockFile(String url);
	
	/**
	 * Actions the cache have to perform when an error message is received.  
	 * @param prefixError
	 */
	public void handlerError(int prefixError);
	
	/**
	 * Actions the cache have to perform when a push new file message is received. 
	 * @param url
	 */
	public void handlerPushNewFile(String url);
}
