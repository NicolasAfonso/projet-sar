package client;

import java.util.LinkedList;
import java.util.List;

import document.TestDocument;

public class clientTest implements I_APICache {
	
	private String currentFile = null;
	private boolean first = true;
	private Cache cache;
	private static int id ;
	LinkedList<String> filesAvailable ; 

	public clientTest(String[] args){
		cache = new Cache(args,this);
		setId(Integer.parseInt(args[0]));
		filesAvailable = new LinkedList<String>();
	}
	@Override
	public void handlerAddFile(boolean state) {
		currentFile = filesAvailable.getFirst();
		filesAvailable.removeFirst();
		filesAvailable.addLast(currentFile);
		cache.lockFile(currentFile);
		
	}
	
	@Override
	public void handlerUnlockFile() {
		currentFile = filesAvailable.getFirst();
		filesAvailable.removeFirst();
		filesAvailable.addLast(currentFile);
		cache.lockFile(currentFile);	
	}
	
	@Override
	public void handlerDeleteFile(boolean state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlerListFile(List<String> urlsAvailable) {
		List<String> urls = urlsAvailable;
		for (String url : urls) {
			if(!filesAvailable.contains(url)){
				filesAvailable.addLast(url);
			}
		}
		
		if(first)
		{
			if(!filesAvailable.contains("TutuTest"+id))
			{
				TestDocument d = new TestDocument("TutuTest"+id, id);
				d.setFile(new String("VIDE").getBytes());
				cache.addFile(d);
				first = false;
			}
			
			if(!filesAvailable.isEmpty() && filesAvailable.contains("TutuTest"+id) )
				{
				currentFile = filesAvailable.getFirst();
				filesAvailable.removeFirst();
				filesAvailable.addLast(currentFile);
				cache.lockFile(currentFile);
				}

		}
	}

	@Override
	public void handlerLockFile(boolean state) {
		cache.downloadFile(currentFile);
		
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
		cache.unlockFile(currentFile);
		
	}

	@Override
	public void handlerServerAvailable(boolean state) {
		// TODO Auto-generated method stub
		
	}
	
	public void start(){
		//cache.listFile();

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
