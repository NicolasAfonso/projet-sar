package server;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import engine.Client;

public class LockManager implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int idLock ;
	private List<Integer> waitLock;
	private String urlD;
	public LockManager(String doc){
		waitLock = new ArrayList<Integer>();
		setUrlD(doc);
		idLock=-1;
	}

	/**
	 * @return the lock
	 */
	public int getLock() {
		return idLock;
	}

	/**
	 * @param lock the lock to set
	 */
	public void setLock(int idClient) {
		this.idLock = idClient;
	}

	/**
	 * @return the waitLock
	 */
	public List<Integer> getWaitLock() {
		return waitLock;
	}

	/**
	 * @param waitLock the waitLock to set
	 */
	public void setWaitLock(List<Integer> waitLock) {
		this.waitLock = waitLock;
	}

	public void addWaitLock(int c) {
		waitLock.add(c);
	}
	
	public Integer nextLock(){

		if(!waitLock.isEmpty())
		{
			int c = waitLock.get(0);
			waitLock.remove(0);
			return c;
		}
		return -1;

	}

	/**
	 * @return the urlD
	 */
	public String getUrlD() {
		return urlD;
	}

	/**
	 * @param urlD the urlD to set
	 */
	public void setUrlD(String urlD) {
		this.urlD = urlD;
	}
}
