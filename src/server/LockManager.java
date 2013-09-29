package server;
import java.util.List;

import engine.Client;

public class LockManager {

	private Client lock ;
	private List<Client> waitLock;
	
	public LockManager(){
		
	}

	/**
	 * @return the lock
	 */
	public Client getLock() {
		return lock;
	}

	/**
	 * @param lock the lock to set
	 */
	public void setLock(Client lock) {
		this.lock = lock;
	}

	/**
	 * @return the waitLock
	 */
	public List<Client> getWaitLock() {
		return waitLock;
	}

	/**
	 * @param waitLock the waitLock to set
	 */
	public void setWaitLock(List<Client> waitLock) {
		this.waitLock = waitLock;
	}
}
