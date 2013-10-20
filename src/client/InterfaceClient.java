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
				System.out.println("Enter your command :");
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
					doc.setFile(new String("DocumentContent").getBytes());
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
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					cache.downloadFile(message);
					break;
				case "unlockfile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					cache.unlockFile(message);
					break;
				case "deletefile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					cache.deleteFile(message);
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
	public void handlerAddFile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerDeleteFile() {
		// TODO Auto-generated method stub

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
	public void handlerLockFile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerUpdateFile() {


	}
	//
	//	private void leave() {
	//		// TODO Auto-generated method stub
	//
	//	}
	//
	//	private void accessFile() {
	//		// TODO Auto-generated method stub
	//
	//	}
	//
	//	private void remove() {
	//		// TODO Auto-generated method stub
	//
	//	}
	//
	//	private void addFile(I_Document doc) {
	//		cache.addFile(doc);
	//
	//	}

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

	}

	@Override
	public void handlerReceivedFile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerUnlockFile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerError(String errorType) {
		
		
		if (errorType.equals("ID"))
		{
		System.out.println("Your ID is already used by another client. Connection will be aborted.");
		System.exit(0);
		}
	}

	@Override
	public void handlerPushNewFile() {
		// TODO Auto-generated method stub
		
	}


}
