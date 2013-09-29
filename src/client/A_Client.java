package client;

import java.util.ArrayList;
import java.util.List;

import document.I_Document;
import engine.I_NioEngine;

public class A_Client implements I_ClientHandler{

	private int id ; 
	private Cache cache ;
	private List<String> urls = new ArrayList<String>();
	private I_NioEngine nio ;
	
}
