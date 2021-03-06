package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import document.Document;
import document.I_Document;
import document.TestDocument;
import engine.Client;
import engine.I_NioEngine;
import engine.NioEngine;
import engine.TYPE_MSG;
public class Server implements I_ServerHandler,Runnable{

	private HashMap<String,I_Document> documents ;
	private HashMap<I_Document,LockManager> locks;
	private HashMap<Integer, I_Document> docsLockClient ;

	private HashMap<Integer, SocketChannel> idClients;

	private I_NioEngine nio ;
	private Thread nioT ;
	private ByteBuffer tmp ;
	private static final Logger logger = Logger.getLogger(NioEngine.class);
	private File backupDirectory ;
	private File locksDirectory;
	private InetAddress addrServer ;
	private int portServer;
	private Scanner cmd;
	public Server(String[] args)
	{
		documents = new HashMap<>();
		locks = new HashMap<>();
		docsLockClient = new HashMap<>();

		idClients = new HashMap<>();

	
		backupDirectory = new File("backup");
		locksDirectory = new File ("lock");

		try {
			addrServer = InetAddress.getByName("localhost");
			portServer = Integer.parseInt(args[0]);
			if(portServer < 8888)
			{
				logger.error("We can't used reserved port");
				System.exit(0);
			}
			if (! backupDirectory.exists()) {
				backupDirectory.mkdir();
				logger.info(backupDirectory.getName()+" directory has been created on server.");
			}
			if (! locksDirectory.exists()){
				locksDirectory.mkdir();
				logger.info(locksDirectory.getName()+" directory has been created on server.");
			}

		} catch (UnknownHostException e) {
			logger.error(" Unknown Host");
			System.exit(0);
		}  
	}


	@Override
	public void run() {
		cmd = new Scanner(System.in);
		String message ;
		boolean running = true;
		while(running) {
			cmd.nextLine();
			System.out.println("Enter your command :");
			message = cmd.nextLine();
			switch(message)
			{
			case "listclient":
				List<Client> clients = nio.getClients();
				System.out.println("Clients list :");
				for(Client c:clients)
				{
					System.out.println(c.getId());
				}
				break;
			case "listfile" :
				List<String> listFic = listDirectory(backupDirectory);
				System.out.println("List available file");
				for(String s:listFic)
				{
					System.out.println(s);
				}
				break;
			case "listLock":
				break;
			case "kill" :
				running = false;
				break;
			}
		}
		logger.info("Bye");
		System.exit(0);

	}
	private void init()
	{
		nio= new NioEngine();
		try {
			nio.initializeAsServer(addrServer, portServer, this);
		} catch (IOException e) {
			logger.error(" Error to initialize Server");
			System.exit(0);
		}
		/*
		 * Restore document
		 */
		logger.info("Server restore documents  ");
		List<String> listFic = listDirectory(backupDirectory);
		for(String nameFic: listFic)
		{
			if(!nameFic.contains(".bak"))
			{
			I_Document docRestore = this.restoreDocument(backupDirectory,nameFic);
			
			if(docRestore==null)
			{
				logger.error("Erase the last document beacause is corrupted");
				docRestore = this.restoreDocument(backupDirectory,nameFic+".bak");
				logger.warn("Restore document "+docRestore.getUrl()+" from backup and push clients ");
				this.backupDocument(backupDirectory,docRestore,false);
				documents.put(docRestore.getUrl(),docRestore);
			}
			else
			{
				logger.debug("Restore document "+docRestore.getUrl()+" and push clients ");
				documents.put(docRestore.getUrl(),docRestore);
			}
		
			}
		}

		logger.info("Server restore locks  ");
		listFic = listDirectory(locksDirectory);
		for(String nameFic: listFic)
		{
			if(!nameFic.contains(".bak"))
			{
				LockManager lockRestore = this.restoreLock(locksDirectory,nameFic);
				if(lockRestore == null)
				{
					logger.error("Erase the last lock beacause is corrupted");
					lockRestore = this.restoreLock(locksDirectory,nameFic+".bak");
					lockRestore.getWaitLock().clear();
					logger.warn("Restore lock on "+lockRestore.getUrlD()+"from backup");
					locks.put(documents.get(lockRestore.getUrlD()),lockRestore);
					if(lockRestore.getLock() != -1)
					{
						docsLockClient.put(lockRestore.getLock(),documents.get(lockRestore.getUrlD()));
					}
					this.backupLock(locksDirectory,locks.get(documents.get(nameFic)),false);
					
				}else
				{
					lockRestore.getWaitLock().clear();
					logger.debug("Restore lock on "+lockRestore.getUrlD());
					locks.put(documents.get(lockRestore.getUrlD()),lockRestore);
					if(lockRestore.getLock() != -1)
					{
						docsLockClient.put(lockRestore.getLock(),documents.get(lockRestore.getUrlD()));
					}
	
				}
			}
			
			

		}
		nioT = new Thread(nio);
		nioT.start();
		
		logger.info("Server start on  "+addrServer.toString()+":"+portServer);
	}



	public static void main(String[] args) {
		try {
			Server s =new Server(args);
			s.init();
			s.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivedMSG(byte[] data, TYPE_MSG type, SocketChannel socketChannel) {

		switch (type)
		{
		case ERROR :
			break;
		case HELLO_CLIENT : 
			receivedHelloClient(data,socketChannel);
			break;
		case LIST_FILE :
			reveivedListFile(data,socketChannel);
			break;
		case UPLOAD :
			receivedUploadClient(data,socketChannel);
			break;
		case DOWNLOAD : 
			receivedDownloadClient(data,socketChannel);
			break;
		case LOCK:
			receivedLockClient(data,socketChannel);
			break;
		case UNLOCK :
			receivedUnlockClient(data,socketChannel);
			break;
		case DELETE : 
			receivedDeleteFile(data,socketChannel);
			break;

		default :
			break;

		}

	}

	private void receivedUnlockClient(byte[] data, SocketChannel socketChannel) {
		Client client = nio.getClient(socketChannel);
		if(docsLockClient.containsKey(client.getId()))
		{
			tmp = ByteBuffer.allocate(data.length);
			tmp.put(data);
			tmp.rewind();
			int urlSize= tmp.getInt(0);
			byte[] urlb = new byte[urlSize];
			tmp.position(4);
			tmp.get(urlb, 0, urlSize);
			String url = new String(urlb);
			I_Document docRequest = documents.get(url);
			LockManager lock = locks.get(docRequest);
			if(lock.getLock() == client.getId())
			{
				logger.info("Received unlock on "+ docRequest.getUrl() + "form client "+client.getId());
				docsLockClient.remove(client.getId());
				nio.send(this.getClientSocketChannel(client.getId()),docRequest.getUrl().getBytes(),TYPE_MSG.ACK_UNLOCK);
				this.backupLock(locksDirectory,lock,true);
				int nextClient = lock.nextLock();
				if(nextClient !=-1)
				{
					lock.setLock(nextClient);
					logger.info("Give lock on "+docRequest.getUrl()+" to "+nextClient);
					docsLockClient.put(nextClient,docRequest);
					SocketChannel socketChannelClient = this.getClientSocketChannel(nextClient);
					while(socketChannelClient == null && nextClient !=-1)
					{
						logger.error("Can not give lock on "+docRequest.getUrl()+" to "+nextClient +" because the client "+nextClient+" is not available");
						docsLockClient.remove(nextClient);
						nextClient = lock.nextLock();
						lock.setLock(nextClient);
						logger.warn("Give lock on "+docRequest.getUrl()+" to "+nextClient);
						socketChannelClient = this.getClientSocketChannel(nextClient);
						docsLockClient.put(nextClient,docRequest);
					}

					if(socketChannelClient !=null)
					{
						nio.send(this.getClientSocketChannel(nextClient),docRequest.getUrl().getBytes(),TYPE_MSG.ACK_LOCK);
						logger.debug("Send lock for "+docRequest.getUrl()+" to "+nextClient);
					}
					else
					{
						lock.setLock(-1);
						logger.warn("No client available, set lock to -1");
					}


				}
				else
				{
					lock.setLock(-1);
				}
				this.backupLock(locksDirectory,lock,false);
			}
			else{
				logger.error("Unlock refused for "+docRequest.getUrl()+" to "+client.getId());
				nio.send(this.getClientSocketChannel(client.getId()),ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
			}
		}
		else
		{
			logger.warn("Unlock refused from "+client.getId());
			nio.send(this.getClientSocketChannel(client.getId()),ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
		}

	}


	private void receivedLockClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		tmp = ByteBuffer.allocate(data.length);
		tmp.put(data);
		tmp.rewind();
		int urlSize= tmp.getInt(0);
		byte[] urlb = new byte[urlSize];
		tmp.position(4);
		tmp.get(urlb, 0, urlSize);
		String url = new String(urlb);
		I_Document doc = documents.get(url);
			if(doc != null)
			{
				LockManager lock = locks.get(doc);
				this.backupLock(locksDirectory,lock,true);
				if(lock.getLock()==-1)
				{
					this.backupLock(locksDirectory,lock,true);
					lock.setLock(c.getId());
					docsLockClient.put(c.getId(),doc);
					this.backupLock(locksDirectory, lock,false);
					nio.send(this.getClientSocketChannel(c.getId()),doc.getUrl().getBytes(),TYPE_MSG.ACK_LOCK);
					logger.info("Give Lock on "+ doc.getUrl()+" to " +c.getId());	
				}
				else 
				{
					if(lock.getLock()==c.getId()){
						docsLockClient.put(c.getId(),doc);
						logger.debug("Already lock for this client");
						nio.send(this.getClientSocketChannel(c.getId()),doc.getUrl().getBytes(),TYPE_MSG.ACK_LOCK);
					}
					else{
						if(lock.getWaitLock().contains(c.getId()))
						{
							logger.debug("Already existing waitLock for this client");
							checkLockClient(lock,doc);
						}
						else
						{
							lock.addWaitLock(c.getId());
							this.backupLock(locksDirectory, lock,false);
							logger.info("Wait Lock on "+ doc.getUrl() + " by Client "+c.getId());
							checkLockClient(lock,doc);
						}
					}
				}
			}
			else
			{
				logger.error("Erreur Lock : File not found");
			}
		}



	


	private void reveivedListFile(byte[] data, SocketChannel socketChannel) {
		logger.info("Received LIST_FILE");
		Object[] dop = documents.values().toArray();
		List<String> listFile = new ArrayList<String>();
		for(Object o : dop)
		{
			listFile.add(((I_Document)o).getUrl());
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(listFile);
			byte[] docs = bos.toByteArray();
			out.close();
			bos.close();
			nio.send(socketChannel,docs,TYPE_MSG.ACK_LIST_FILE);

		} catch (IOException e) {
			logger.error("Error received List File");
			System.exit(0);
		}	
	}

	/**
	 * Callback used when a client delete a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedDeleteFile(byte[] data, SocketChannel socketChannel) {
		Client client = nio.getClient(socketChannel);
		if(docsLockClient.containsKey(client.getId()))
		{
			tmp = ByteBuffer.allocate(data.length);
			tmp.put(data);
			tmp.rewind();
			int urlSize= tmp.getInt(0);
			byte[] urlb = new byte[urlSize];
			tmp.position(4);
			tmp.get(urlb, 0, urlSize);
			String url = new String(urlb);
			I_Document doc = documents.get(url);
			LockManager lock = locks.get(doc);
			if(client.getId() == doc.getOwner() && lock.getLock() == client.getId() )
			{
				if(doc != null)
				{
					documents.remove(url);
					nio.push(doc,TYPE_MSG.DELETE);
					this.deleteFile(backupDirectory,url);
					this.deleteFile(locksDirectory, url);
					logger.info("Remove document "+doc.getUrl()+" and push clients");
				}

			}else
			{
				logger.warn("Received bad remove request "+ "from "+client.getId());
				nio.send(this.getClientSocketChannel(client.getId()),ByteBuffer.allocate(4).putInt(4).array(),TYPE_MSG.ERROR);
			}

		}else
		{
			logger.warn("Received bad remove request "+ "from "+client.getId());
			nio.send(this.getClientSocketChannel(client.getId()),ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
		}


	}

	/**
	 * Callback used when a client download a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedDownloadClient(byte[] data, SocketChannel socketChannel) {
		Client client = nio.getClient(socketChannel);
		if(docsLockClient.containsKey(client.getId()))
		{
			tmp = ByteBuffer.allocate(data.length);
			tmp.put(data);
			tmp.rewind();
			int urlSize= tmp.getInt(0);
			byte[] urlb = new byte[urlSize];
			tmp.position(4);
			tmp.get(urlb, 0, urlSize);
			String url = new String(urlb);
			I_Document doc = documents.get(url);
			LockManager lock = locks.get(doc);
			if(lock.getLock() == client.getId() )
			{
				if(doc != null)
				{
					byte[] docD = I_DocumentToByte(doc);
					nio.send(socketChannel,docD,TYPE_MSG.ACK_DOWNLOAD);
					logger.info("Send document "+doc.getUrl()+" to "+client.getId());
				}
				else
				{
					logger.warn("Received bad document request "+ "from "+client.getId());
					nio.send(socketChannel,ByteBuffer.allocate(4).putInt(3).array(),TYPE_MSG.ERROR);
				}
			}
			else
			{
				logger.warn("Received bad document request "+ "from "+client.getId());
				nio.send(socketChannel,ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
			}
		}	
		else
		{
			logger.warn("Received bad document request "+ "from "+client.getId());
			nio.send(socketChannel,ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
		}


	} 

	/**
	 * Callback used when a client upload a document
	 * @param data
	 * @param socketChannel
	 */
	private void receivedUploadClient(byte[] data, SocketChannel socketChannel) {
		Client client = nio.getClient(socketChannel);
		logger.info("Received UPLOAD form "+client.getId());
		I_Document docReceived = bytesToI_Document(data);
		I_Document doc = documents.get(docReceived.getUrl()); // we check if the document already exist on the server
		if(doc == null)
		{
			logger.info("Create new document "+docReceived.getUrl()+" and push clients ");
			documents.put(docReceived.getUrl(),docReceived);
		//	docsLockClient.put(nio.getClient(socketChannel).getId(), docReceived);
			locks.put(docReceived,new LockManager(docReceived.getUrl()));
			this.backupDocument(backupDirectory,docReceived,false);
			this.backupDocument(backupDirectory,docReceived,true);
			this.backupLock(locksDirectory,locks.get(docReceived),false);
			this.backupLock(locksDirectory,locks.get(docReceived),true);
			nio.push(docReceived,TYPE_MSG.PUSH_NEW_FILE);
		}
		else
		{
			if(docsLockClient.containsKey(client.getId()))
			{
				if(doc.getVersionNumber() <= docReceived.getVersionNumber())
				{
					this.backupDocument(backupDirectory,doc,true);
					logger.info("Received : "+ docReceived.getOwner() +"-"+docReceived.getUrl()+"-"+docReceived.getVersionNumber() );
					doc.setFile(docReceived.getFile());
					doc.setVersionNumber(docReceived.getVersionNumber());
					documents.put(doc.getUrl(),doc);
					nio.send(socketChannel,doc.getUrl().getBytes(),TYPE_MSG.ACK_UPLOAD);
					this.backupDocument(backupDirectory,doc,false);
				}
				else
				{
					logger.warn("Received old version document "+ docReceived.getUrl() +" from "+client.getId());
					nio.send(socketChannel, ByteBuffer.allocate(4).putInt(2).array(), TYPE_MSG.ERROR);
				}
			}
			else
			{
				logger.warn("Received bad document request "+ "from "+client.getId());
				nio.send(socketChannel,ByteBuffer.allocate(4).putInt(1).array(),TYPE_MSG.ERROR);
			}
		}
	}


	private SocketChannel getClientSocketChannel(int nextClient) {
		List<Client> clients = nio.getClients();
		for(Client c:clients)
		{
			if(c.getId()==nextClient)
			{
				return c.getSocketChannel();
			}
		}
		return null;
	}


	/**
	 * Callback used when a new client is connected on the server content
	 * @param data
	 * @param socketChannel
	 */

	private void receivedHelloClient(byte[] data, SocketChannel socketChannel) {
		Client c = nio.getClient(socketChannel);
		tmp = ByteBuffer.allocate(4);
		tmp.put(data);
		tmp.rewind();
		int i = tmp.getInt();

		if(idClients.containsKey(i)) {// check if a client with the same id don't exist already
			c.setId(-1);
			logger.error(new String("A client with the same ID already exist. Connection refused."));
			nio.send(socketChannel, ByteBuffer.allocate(4).putInt(0).array(), TYPE_MSG.ERROR);

		}
		else {
			idClients.put(i, socketChannel);

			c.setId(i);
			nio.send(socketChannel, data, TYPE_MSG.ACK_HELLO_CLIENT);
			logger.info("Received HELLO_CLIENT form "+c.getId());
		}
	}


	/*
	 * Tools
	 */
	/**
	 * Transform a bytes array in I_Document object
	 * @param data
	 * @return
	 */
	private I_Document bytesToI_Document(byte[] data){
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			I_Document doc = (I_Document) in.readObject(); 
			bis.close();
			in.close();
			return doc;
		} catch (IOException e) {
			logger.error("Error to translate Bytes in I_Document");
		} catch (ClassNotFoundException e) {
			logger.error("Error to translate Bytes in I_Document");
		}
		return null;
	}

	/**
	 * Transform a I_Document in bytes array
	 * @param doc
	 * @return
	 */
	private byte[] I_DocumentToByte(I_Document doc){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(doc);
			byte[] bytes = bos.toByteArray();
			out.close();
			bos.close();
			return bytes;
		} catch (IOException e) {
			logger.error("Error to translate I_Document in Bytes");
		}
		return null;
	}

	private void backupDocument(File dir,I_Document doc, boolean backup)
	{
		FileOutputStream fichier;
		try {
			if(backup)
			{
				fichier = new FileOutputStream(dir.getName()+"/"+doc.getUrl()+".bak");
			}
			else
			{
				fichier = new FileOutputStream(dir.getName()+"/"+doc.getUrl());
			}
			ObjectOutputStream oos = new ObjectOutputStream(fichier);
			oos.writeObject(doc);
			oos.close();
			fichier.close();
			logger.debug("Backup DOC:"+doc.getUrl());
		} catch (FileNotFoundException e) {
			logger.error("File not found",e);

		} catch (IOException e) {
			logger.error("Error backup",e);
		}

	}

	private I_Document restoreDocument(File dir,String backupName)
	{
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(dir.getName()+"/"+backupName));
			Object o = ois.readObject();
			ois.close();
			if(o instanceof Document)
			{
				Document doc = (Document) o;
				logger.debug("Test serialization "+ doc.getOwner() + "-" + doc.getUrl()+"-"+doc.getVersionNumber());
				return doc;
			}
			else if(o instanceof TestDocument)
			{
				TestDocument doc = (TestDocument) o ;
				logger.debug("Test serialization "+ doc.getOwner() + "-" + doc.getUrl());
				return doc;
			}
		} catch (FileNotFoundException e) {
			logger.error("Error restore",e.getCause());
		} catch (EOFException e){
			logger.error("Error restore",e.getCause());
		}
		catch (IOException e) {
			logger.error("Error restore",e.getCause());

		} catch (ClassNotFoundException e) {
			logger.error("Error restore",e.getCause());
		}
		return null;
	}

	private void backupLock(File locksDirectory, LockManager lockManager, boolean backup) {
		FileOutputStream fichier;
		try {
			if(backup)
			{
				fichier = new FileOutputStream(locksDirectory.getName()+"/"+lockManager.getUrlD()+".bak");
			}
			else
			{
				fichier = new FileOutputStream(locksDirectory.getName()+"/"+lockManager.getUrlD());
			}

			ObjectOutputStream oos = new ObjectOutputStream(fichier);
			oos.writeObject(lockManager);
			oos.close();
			fichier.close();
			logger.debug("Backup LOCK :"+lockManager.getUrlD());
		} catch (FileNotFoundException e) {
			logger.debug("File not found",e);

		} catch (IOException e) {
			logger.error("Error backup",e);
		}

	}

	private LockManager restoreLock(File locksDirectory, String nameFic) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(locksDirectory.getName()+"/"+nameFic));
			Object o = ois.readObject();
			ois.close();
			if(o instanceof LockManager)
			{
				LockManager lock = (LockManager) o;
				logger.debug("Test serialization "+ lock.getUrlD());
				return lock;
			}
		} catch (FileNotFoundException e) {
			logger.error("Error restore",e.getCause());
		} catch (IOException e) {
			logger.error("Error restore",e.getCause());

		} catch (ClassNotFoundException e) {
			logger.error("Error restore",e.getCause());
		}
		return null;
	}

	private void deleteFile(File dir,String file){
		File f = new File(dir.getName()+"/"+file);
		f.delete();
	}

	private List<String> listDirectory(File backupDirectory) {
		//File file = new File(backupDirectory2);
		File[] files = backupDirectory.listFiles();
		List<String> listFile = new ArrayList<String>();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile() == true) {
					listFile.add(files[i].getName());
				}
			}
			return listFile;
		}
		return listFile;
	}
	
	private void checkLockClient(LockManager lock, I_Document docRequest) {

		int client = lock.getLock();
		SocketChannel socketChannelClient = this.getClientSocketChannel(client);

		if(socketChannelClient == null)
		{
			int nextClient = lock.nextLock();
			while(docsLockClient.get(nextClient) != null)
			{
				lock.addWaitLock(nextClient);
				nextClient = lock.nextLock();
			}
			if(nextClient !=-1)
			{
				lock.setLock(nextClient);
				logger.info("Give lock on "+docRequest.getUrl()+" to "+nextClient);
				docsLockClient.put(nextClient,docRequest);
				socketChannelClient = this.getClientSocketChannel(nextClient);
				while(socketChannelClient == null && nextClient !=-1)
				{
					logger.warn("Can not give lock on "+docRequest.getUrl()+" to "+nextClient +" because the client "+nextClient+" is not available");
					docsLockClient.remove(nextClient);
					nextClient = lock.nextLock();
					lock.setLock(nextClient);
					logger.info("Give lock on "+docRequest.getUrl()+" to "+nextClient);
					socketChannelClient = this.getClientSocketChannel(nextClient);
					docsLockClient.put(nextClient,docRequest);
				}

				if(socketChannelClient !=null)
				{
					nio.send(this.getClientSocketChannel(nextClient),docRequest.getUrl().getBytes(),TYPE_MSG.ACK_LOCK);
					logger.debug("Send lock for "+docRequest.getUrl()+" to "+nextClient);
				}
				else
				{
					lock.setLock(-1);
					logger.warn("No client available, set lock to -1");
				}


			}
		}


	}
	
	@Override
	public void clientDisconnected(Client client) {
		idClients.remove(client.getId());
		logger.warn("Client "+client.getId() +" is disconnected");
		if(docsLockClient.containsKey(client.getId()));
		{
			I_Document doc = docsLockClient.get(client.getId());
			docsLockClient.remove(client.getId());
			LockManager lock = locks.get(doc);
			if(lock != null)
			{
				this.backupLock(locksDirectory,lock,true);
				int nextClient = lock.nextLock();
				while(docsLockClient.get(nextClient) != null)
				{
					lock.addWaitLock(nextClient);
					nextClient = lock.nextLock();
				}
				if(nextClient !=-1)
				{
					lock.setLock(nextClient);
					logger.info("Give lock on "+doc.getUrl()+" to "+nextClient);
					docsLockClient.put(nextClient,doc);
					SocketChannel socketChannelClient = this.getClientSocketChannel(nextClient);
					while(socketChannelClient == null && nextClient !=-1)
					{
						logger.warn("Can not give lock on "+doc.getUrl()+" to "+nextClient +" because the client "+nextClient+" is not available");
						docsLockClient.remove(nextClient);
						nextClient = lock.nextLock();
						lock.setLock(nextClient);
						logger.info("Give lock on "+doc.getUrl()+" to "+nextClient);
						socketChannelClient = this.getClientSocketChannel(nextClient);
						docsLockClient.put(nextClient,doc);
					}

					if(socketChannelClient !=null)
					{
						nio.send(this.getClientSocketChannel(nextClient),doc.getUrl().getBytes(),TYPE_MSG.ACK_LOCK);
						logger.debug("Send lock for "+doc.getUrl()+" to "+nextClient);
					}
					else
					{
						lock.setLock(-1);
						logger.warn("No client available, set lock to -1");
					}


				}
				else
				{
					lock.setLock(-1);
				}
				this.backupLock(locksDirectory,lock,false);
			}


		}
	}

}
