package client;

import java.util.List;


public interface I_APICache {
	public void handlerAddFile();
	public void handlerDeleteFile();
	public void handlerListFile(List<String> urlsAvailable);
	public void handlerLockFile() ;
	public void handlerUpdateFile();
	public void handlerServerAvailable();
	public void handlerReceivedFile();
	public void handlerUnlockFile();
	
	public void handlerError(String prefixError);
	public void handlerPushNewFile();
}
