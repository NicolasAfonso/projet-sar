package client;
import java.util.ArrayList;
import java.util.List;

import document.Document;
import document.I_Document;
public class InterfaceClient {

	private Cache cache;
	private int id ; 
	public InterfaceClient(String[] args){
		cache = new Cache(args);
		setId(Integer.parseInt(args[0])) ;
	}

	public void connect(String[] args){

		cache.init(args);

		}
	
	public static void main(String[] args) {
		try {
			Cache c = new Cache(args); 
			c.main();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void leave() {
		// TODO Auto-generated method stub
		
	}

	private void accessFile() {
		// TODO Auto-generated method stub
		
	}

	private void remove() {
		// TODO Auto-generated method stub
		
	}

	private void addFile(I_Document doc) {
		cache.addFile(doc);
		
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
}
