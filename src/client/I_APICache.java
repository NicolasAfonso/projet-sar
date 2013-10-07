package client;

import java.util.List;


public interface I_APICache {
	public void handlerAddFile(boolean state);

	public void handlerDeleteFile(boolean state);


	public void handlerListFile(List<String> urlsAvailable);


	public void handlerLockFile(boolean state) ; 


	public void handlerUpdateFile(boolean state);
	
	public void handlerServerAvailable(boolean state);
}
