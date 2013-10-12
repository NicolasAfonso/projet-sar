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
	private boolean serverAvailable;
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
//				if(serverAvailable) {
					cmd = new Scanner(System.in);
					String message ;
					System.out.println("Enter your command :");
					message = cmd.nextLine();
					switch(message)
					{
					case "listfile" :
						cache.listFile();
						break ;
					case "addFile" :
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
					case "exit":
						System.out.println("Bye");
						System.exit(0);
						break;
					}
//				}
//				else
//				{
//					System.out.println("Waiting Server....");
//					while(!serverAvailable){};
//				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handlerAddFile(boolean state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerDeleteFile(boolean state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerListFile(List<String> urlsAvailable) {

		if(urlsAvailable.size()==0)
		{
			System.out.println("No File available");
		}
		else
		{
			System.out.println("File List :");
			for (String url : urlsAvailable) {
				System.out.println(url);
			}
		}


	}

	@Override
	public void handlerLockFile(boolean state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlerUpdateFile(boolean state) {
		

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
	public void handlerServerAvailable(boolean state) {
		serverAvailable = state;
	}


}
