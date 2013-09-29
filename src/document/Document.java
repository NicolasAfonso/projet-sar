package document;

import java.io.Serializable;

import engine.Client;

public class Document implements I_Document,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String url ;
	private Client owner; 
	private long versionNumber ;
	private I_Document currentVersion;
	private I_Document savedVersion ;
	
	public Document(String u,Client o){
		
		url = u ; 
		owner = o;
		versionNumber = 0 ;
		currentVersion = this; 
		savedVersion = null ;
		
	}
	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return the owner
	 */
	public Client getOwner() {
		return owner;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(Client owner) {
		this.owner = owner;
	}

	/**
	 * @return the versionNumber
	 */
	public long getVersionNumber() {
		return versionNumber;
	}
	/**
	 * @param versionNumber the versionNumber to set
	 */
	public void setVersionNumber(long versionNumber) {
		this.versionNumber = versionNumber;
	}
	/**
	 * @return the currentVersion
	 */
	public I_Document getCurrentVersion() {
		return currentVersion;
	}
	/**
	 * @param currentVersion the currentVersion to set
	 */
	public void setCurrentVersion(I_Document currentVersion) {
		this.currentVersion = currentVersion;
	}
	/**
	 * @return the savedVersion
	 */
	public I_Document getSavedVersion() {
		return savedVersion;
	}
	/**
	 * @param savedVersion the savedVersion to set
	 */
	public void setSavedVersion(I_Document savedVersion) {
		this.savedVersion = savedVersion;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	
	
}
