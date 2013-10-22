package client;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import document.Document;
import document.I_Document;
public class InterfaceClient implements I_APICache {

	private Cache cache;
	private static int id ;
	private static Scanner cmd; 
	public InterfaceClient(String[] args){
		cache = new Cache(args,this);
		setId(Integer.parseInt(args[0])) ;
	}

	public void connect(String[] args){
		cache.init();
	}

	public static void main(String[] args) {

		InterfaceClient console = new InterfaceClient(args);
		Cache c = console.cache ; 
		c.init();
		console.mainLoop();

	}

	private void mainLoop() {
		try {
			while(true)
			{
				cmd = new Scanner(System.in);
				String message ;
				System.out.println("\nEnter your command :");
				message = cmd.nextLine();
				switch(message)
				{
				case "listfile" :
					cache.listFile();
					break ;
				case "addfile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					I_Document doc = new Document(message, id);
					doc.setFile(new String("DocumentContent"+ message).getBytes());
					cache.addFile(doc);
					break;
				case "lockfile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					cache.lockFile(message);
					break;
				case "updatefile" :
					cache.updateFile();
					break;
				case "downloadfile":
					//System.out.println("Enter File name :");
					//message = cmd.nextLine();
					//cache.downloadFile(message);
					cache.downloadFile();
					break;
				case "unlockfile" :
					//System.out.println("Enter File name :");
					//message = cmd.nextLine();
					cache.unlockFile();
					break;
				case "deletefile" :
					//System.out.println("Enter File name :");
					//message = cmd.nextLine();
					cache.deleteFile();
					break;
				case "openfile" :	// you can only open the file you've locked
					cache.openfile();
					break;
				case "?" :
				case "help" :
					System.out.println("Available commands :");
					System.out.println("- [listfile] give you the list of the available files on server");
					System.out.println("- [addfile] for adding an existing local file on server");
					System.out.println("- [lockfile] request a lock on a server file in order to read or modify it");
					System.out.println("- [updatefile] push your local file to the server");
					System.out.println("- [downloadfile] request the distant file to be downloaded into your cache. You must obtain the lock on this file before (if not an error is thrown).");
					System.out.println("- [unlockfile] indicate to the server you finished to work with the file");
					System.out.println("- [deletefile] request the distant file to be deleted");
					break;
				case "exit":
					System.out.println("Bye");
					System.exit(0);
					break;
				default:
					System.out.println("Your command is not recognised by the system. Type '?' or 'help' for displaying the command list.");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handlerDeleteFile(String url) {
		System.out.println("File "+ url +" has been deleted");

	}
	@Override
	public void handlerListFile(List<String> urlsAvailable) {
		int size = urlsAvailable.size();
		if(size ==0)
		{
			System.out.println("No File available");
		}
		else
		{
			if (size == 1) 
				System.out.println("One file is available on server :");
			else {
				System.out.println(size+" files are available on server :");
			}

			for (String url : urlsAvailable) {
				System.out.println(url);
			}
		}


	}

	@Override
	public void handlerLockFile(String url) {
		System.out.println("File "+ url +" has been locked");

	}

	@Override
	public void handlerUpdateFile(String url) {
		System.out.println("File "+ url +" has been updated");

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

	@Override
	public void handlerServerAvailable() {
		System.out.println("Server is available");
	}
	
	@Override
	public void handlerServerNotAvailable() {
		System.out.println("Server is not available. Please wait until its reboot.");
	}
	
	@Override
	public void handlerReceivedFile(String url) {
		System.out.println("File "+ url +" has been received");

	}

	@Override
	public void handlerUnlockFile(String url) {
		System.out.println("File "+ url +" has been unlocked");

	}

	@Override
	public void handlerError(int errorType) {

		switch (errorType) {	
		case 0 :
			System.out.println("Your ID is already used by another client. Connection will be aborted.");
			System.exit(0);
			break;
		case 1 :
			System.out.println("You have to lock the file before !");
			break;
		case 2:
			System.out.println("You have uploaded a document whose version is older than it is on the server.");
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
		case 7:
			System.out.println("Your cache is empty. Please lock and download a file before.");
			break;
		default :
			System.out.println("Unknown Error");
			break;
		}
		
			
	}

	@Override
	public void handlerPushNewFile(String url) {
		System.out.println("File "+ url +" has been pushed");
		
	}

	@Override
	public void handlerOpenFile(I_Document doc) {
		System.out.println("Content of file "+doc.getUrl());
		System.out.println(new String(doc.getFile()));
		
	}




}
