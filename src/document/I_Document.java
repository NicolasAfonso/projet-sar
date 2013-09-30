package document;

import engine.Client;

public interface I_Document {
	
	public String getUrl();
	public void setUrl(String url);
	public int getOwner();
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(int owner);

	/**
	 * @return the versionNumber
	 */
	public int getVersionNumber();
	/**
	 * @param versionNumber the versionNumber to set
	 */
	public void setVersionNumber(int versionNumber);
	/**
	 * @return the currentVersion
	 */
	public I_Document getCurrentVersion();
	
	/**
	 * @param currentVersion the currentVersion to set
	 */
	public void setCurrentVersion(I_Document currentVersion);
	/**
	 * @return the savedVersion
	 */
	public I_Document getSavedVersion();
	
	/**
	 * @return the file
	 */
	public byte[] getFile() ;

	/**
	 * @param file the file to set
	 */
	public void setFile(byte[] file);

}
