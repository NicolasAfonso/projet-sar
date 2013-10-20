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

import document.TestDocument;

public class ClientTest implements I_APICache {

	private String currentFile = null;
	private boolean first = true;
	private boolean serverfail = false;
	private Cache cache;
	private static int id ;
	private int round = 0 ;
	private LinkedList<String> filesAvailable ; 
	

	public ClientTest(String[] args){
		cache = new Cache(args,this);
		setId(Integer.parseInt(args[0]));
		filesAvailable = new LinkedList<String>();
	}

	@Override
	public void handlerLockFile() {
		cache.downloadFile(currentFile);

	}

	@Override
	public void handlerReceivedFile() {
		if(cache.getCurrentFile() instanceof TestDocument  )
		{

			try {
				ByteArrayInputStream byteIn = new ByteArrayInputStream(cache.getCurrentFile().getFile());
				ObjectInputStream in = new ObjectInputStream(byteIn);
				Map<Integer,Integer> data = (Map<Integer, Integer>) in.readObject();
				data.put(id,round);
				round ++ ;
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(byteOut);
				out.writeObject(data);
				cache.getCurrentFile().setFile(byteOut.toByteArray());
				cache.updateFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	@Override
	public void handlerUpdateFile() {
		cache.unlockFile(currentFile);

	}
	@Override
	public void handlerUnlockFile() {
		currentFile = filesAvailable.getFirst();
		filesAvailable.removeFirst();
		filesAvailable.addLast(currentFile);
		cache.lockFile(currentFile);	
	}

	@Override
	public void handlerDeleteFile() {
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
		if(serverfail)
		{
			currentFile = filesAvailable.getLast();
			cache.lockFile(currentFile);	
			serverfail = false;
		}
		if(first)
		{
			if(!filesAvailable.contains("TutuTest"+id))
			{
				try {
					TestDocument d = new TestDocument("TutuTest"+id, id);
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
				currentFile = filesAvailable.getFirst();
				filesAvailable.removeFirst();
				filesAvailable.addLast(currentFile);
				cache.lockFile(currentFile);
			}
			first= false;
		}

	}

	@Override
	public void handlerServerAvailable() {
		if(!first)
			{serverfail = true;}

	}

	public static void main(String[] args) {

		ClientTest client = new ClientTest(args);
		Cache c = client.cache ; 
		c.init();

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
				System.out.println("You have to lock the file before !");
			case 2:
				System.out.println("You have uploaded a document whose version is older than it is on the server.");
			case 3:
				System.out.println("The document you requested is not available.");
			case 4:
				System.out.println("You do not have the permission to remove this file. Only the author can delete it.");
			}
		
	}

	@Override
	public void handlerPushNewFile(String url) {
		if(url.equals("TutuTest"+id))
		{
			filesAvailable.addLast(url);
			currentFile = filesAvailable.getFirst();
			filesAvailable.removeFirst();
			filesAvailable.addLast(currentFile);
			cache.lockFile(currentFile);
		}
		else
		{
			filesAvailable.addLast(url);
			currentFile = filesAvailable.getFirst();
			filesAvailable.removeFirst();
			filesAvailable.addLast(currentFile);
			cache.lockFile(currentFile);
		}
	}


}
