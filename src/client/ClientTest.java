package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import document.I_Document;
import document.TestDocument;

public class ClientTest implements I_APICache {

	private String currentFile = null;
	private boolean first = true;
	private boolean serverfail = false;
	private Cache cache;
	private static int id ;
	private int round = 0 ;
	private String firstDoc;
	private LinkedList<String> filesAvailable ; 
	private List<String> alreadyVisted;
	private List<String> alreadyVistedbyTour;
	private enum State{LOCK,UNLOCK,DOWNLOAD,UPLOAD};
	private State state;
	
	public ClientTest(String[] args){
		cache = new Cache(args,this);
		setId(Integer.parseInt(args[0]));
		filesAvailable = new LinkedList<String>();
		alreadyVisted = new ArrayList<String>();
		alreadyVistedbyTour = new ArrayList<String>();
	}

	@Override
	public void handlerLockFile(String url) {
		
		System.out.println("File "+ url +" has been locked");
		if(state == State.UPLOAD)
		{
			cache.updateFile();
		}
		if(state == State.DOWNLOAD)
		{
			cache.downloadFile();
		}else
		{
			cache.downloadFile();
			state = State.DOWNLOAD;
		}
		
		

	}

	@Override
	public void handlerReceivedFile(String url) {
		state = State.DOWNLOAD;
		System.out.println("File "+ url +" has been received");
		if(cache.getCurrentFile() instanceof TestDocument  )
		{

			try {
				ByteArrayInputStream byteIn = new ByteArrayInputStream(cache.getCurrentFile().getFile());
				ObjectInputStream in = new ObjectInputStream(byteIn);
				Map<Integer,Integer> data = (Map<Integer, Integer>) in.readObject();
				if(first)
				{
					int r = -1;
					r = data.get(id);
					if(r!=-1)
					{
					  round = r+1; 
					  first= false;	
					}
									
				}
				if(data.containsKey(id))
				{
					if(data.get(id) != (round-1) && alreadyVisted.contains(url))
					{
						System.err.println("ERROR CONCURENCY" + round +":"+(data.get(id)));
						System.exit(-1);
						
					}
					else
					{
						alreadyVisted.add(url);
					}
				}

				data.put(id,round);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(byteOut);
				out.writeObject(data);
				cache.getCurrentFile().setFile(byteOut.toByteArray());
				cache.updateFile();
				alreadyVistedbyTour.add(url);
				state = State.UPLOAD;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void handlerUpdateFile(String url) {
		state = State.UNLOCK;
		System.out.println("File "+ url +" has been updated");
		
		if(firstDoc.equals(url))
		{
			round ++ ;
			alreadyVistedbyTour.clear();
		}
		cache.unlockFile();


	}
	@Override
	public void handlerUnlockFile(String url) {
		state = State.UNLOCK;
		currentFile=null;
		System.out.println("File "+ url +" has been locked");
		alreadyVistedbyTour.add(url);
		currentFile = filesAvailable.getFirst();
		filesAvailable.removeFirst();
		filesAvailable.addLast(currentFile);
		cache.lockFile(currentFile);
		state = State.LOCK;
	}

	@Override
	public void handlerDeleteFile(String url) {
		System.out.println("File "+ url +" has been deleted");

	}

	@Override
	public void handlerListFile(List<String> urlsAvailable) {
		List<String> urls = urlsAvailable;
		for (String url : urls) {
			if(!filesAvailable.contains(url)){
				filesAvailable.addLast(url);
			}
		}
		if(serverfail)
		{	
			switch(state){
			case LOCK:
				serverfail = false;
				cache.lockFile(currentFile);
				break;
			case DOWNLOAD : 
				serverfail = false;
				cache.downloadFile();
				break;
			case UPLOAD :
				serverfail = false;
				cache.updateFile();
				break;
			case UNLOCK:
				serverfail = false;
				cache.unlockFile();
				break;
			default:
				System.err.println("Default");
				System.exit(-1);
				break;
				}
			
		}
		if(first)
		{
			if(!filesAvailable.contains("TutuTest"+id))
			{
				try {
					firstDoc = "TutuTest"+id ;
					TestDocument d = new TestDocument(firstDoc, id);
					Map<Integer, Integer> data = new HashMap<Integer, Integer>();
					data.put(id,round);
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					ObjectOutputStream out = new ObjectOutputStream(byteOut);
					out.writeObject(data);
					d.setFile(byteOut.toByteArray());
					round ++ ;
					cache.addFile(d);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			else
			{
				firstDoc = filesAvailable.getFirst();
				currentFile = filesAvailable.getFirst();
				filesAvailable.removeFirst();
				filesAvailable.addLast(currentFile);
				state = State.LOCK;
				cache.lockFile(currentFile);
			}
		}

	}

	@Override
	public void handlerServerAvailable() {
		System.out.println("Server is available");

	}
	
	@Override
	public void handlerServerNotAvailable() {
		System.out.println("Server is not available");
		serverfail = true;
		
	}

	public static void main(String[] args) {

		ClientTest client = new ClientTest(args);
		Cache c = client.cache ; 
		c.init();

	}
	/**
	 * @return the idServer is available

	 */
	public static int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public static void setId(int id) {
		ClientTest.id = id;
	}

	@Override
	public void handlerError(int prefixError) {
	switch (prefixError) {
			
			case 0 :
				System.out.println("Your ID is already used by another client. Connection will be aborted.");
				System.exit(0);
				break;
			case 1 :
				if(state == State.UNLOCK)
				{
					currentFile = filesAvailable.getFirst();
					filesAvailable.removeFirst();
					filesAvailable.addLast(currentFile);
					state = State.LOCK;
					cache.lockFile(currentFile);
				}
				if(state == State.UPLOAD)
				{
					cache.lockFile(currentFile);
				}
				if(state == State.DOWNLOAD)
				{
					cache.lockFile(currentFile);
				}
				break;
			case 2:
				System.out.println("You have uploaded a document whose version is older than it is on the server.");
				cache.unlockFile();
				break;
			case 3:
				System.out.println("The document you requested is not available.");
				break;
			case 4:
				System.out.println("You do not have the permission to remove this file. Only the author can delete it.");
				break;
			case 5:
				System.out.println("A file with the same name already exist on server. Please change the file name if you think it is a different one.");
				break;
			case 6 : 
				errorSix();
				break ;
			case 7 :
				
				break;
			default :
				System.out.println("Unknown Error");
				break;
			}
		
	}
	
	private void errorSix(){
		switch(state){
		case LOCK:
			cache.lockFile(currentFile);
			break;
		case DOWNLOAD : 
			cache.downloadFile();
			break;
		case UPLOAD :
			cache.downloadFile();
			break;
		case UNLOCK:
			cache.unlockFile();
			break;
		default:
			System.err.println("Default");
			System.exit(-1);
			break;
			}
	}
	
	@Override
	public void handlerPushNewFile(String url) {
		System.out.println("File "+ url +" has been pushed");
		filesAvailable.addLast(url);
		if(url.equals(firstDoc))
		{
			currentFile = filesAvailable.getFirst();
			filesAvailable.removeFirst();
			filesAvailable.addLast(currentFile);
			cache.lockFile(currentFile);
		}
	}

	@Override
	public void handlerOpenFile(I_Document tmpD) {
		// TODO Auto-generated method stub
		
	}




}
