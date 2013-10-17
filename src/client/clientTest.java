package client;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import document.I_Document;
import document.TestDocument;

public class clientTest implements I_APICache {
	
	private Cache cache;
	private static int id ;
	List<String> filesAvailable ; 

	public clientTest(String[] args){
		cache = new Cache(args,this);
		setId(Integer.parseInt(args[0]));
	}
	@Override
	public void handlerAddFile(boolean state) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handlerUnlockFile() {
		cache.lockFile("TutuTest");
		
	}
	
	@Override
	public void handlerDeleteFile(boolean state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlerListFile(List<String> urlsAvailable) {
		filesAvailable = urlsAvailable;
	}

	@Override
	public void handlerLockFile(boolean state) {
		cache.downloadFile("TutuTest");
		
	}
	
	@Override
	public void handlerReceivedFile() {
		if(cache.getCurrentFile() instanceof TestDocument  )
		{
			byte[] b = cache.getCurrentFile().getFile();
			String old = new String(b);
			System.out.println("Received : " + old);
			String test = "tst"+cache.getCurrentFile().getVersionNumber();
			cache.getCurrentFile().setFile(test.getBytes());
			cache.updateFile();
		}
		
	}
	
	@Override
	public void handlerUpdateFile(boolean state) {
		cache.unlockFile("TutuTest");
		
	}

	@Override
	public void handlerServerAvailable(boolean state) {
		// TODO Auto-generated method stub
		
	}
	
	public void start(){
		cache.listFile();
		while(filesAvailable.size()<1){
			cache.listFile();
		}
//		cache.unlockFile(filesAvailable.get(0));
//		TestDocument d = new TestDocument("TutuTest", id);
		cache.lockFile("TutuTest");
	}
	public static void main(String[] args) {

		clientTest client = new clientTest(args);
		Cache c = client.cache ; 
		c.init();
		client.start();


	}
	/**
	 * @return the id
	 */
	public static int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public static void setId(int id) {
		clientTest.id = id;
	}


}
