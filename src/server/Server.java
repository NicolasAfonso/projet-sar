package server;
import java.util.HashMap;

import document.Document;
import document.I_Document;
import engine.Client;
import engine.I_NioEngine;
public class Server {

	private HashMap<String,I_Document> documents ;
	private HashMap<I_Document,LockManager> locks;
	private HashMap<Client, Document> docsClient ;
	private I_NioEngine nio ;
}
