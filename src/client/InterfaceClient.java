package client;
import java.util.List;
import java.util.Scanner;

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
		cache.init(args);
	}

	public static void main(String[] args) {
		try {
			InterfaceClient console = new InterfaceClient(args);
			Cache c = console.cache ; 
			c.init(args);
			cmd = new Scanner(System.in);
			String message ;
			while(true) {
				System.out.println("Enter your command :");
				message = cmd.nextLine();
				switch(message)
				{
				case "listFile" :
					c.listFile();
					break ;
				case "addFile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					I_Document doc = new Document(message, id);
					doc.setFile(new String("DocumentContent").getBytes());
					c.addFile(doc);
					break;
				case "lockFile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					c.lockFile(message);
					break;
				case "updateFile" :
					c.updateFile();
					break;
				case "deleteFile" :
					System.out.println("Enter File name :");
					message = cmd.nextLine();
					c.deleteFile(message);
					break;
				}
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
		// TODO Auto-generated method stub
		
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

	
}
