package engine;

public enum TYPE_MSG{
	ERROR,HELLO_CLIENT,HELLO_SERVER,LOCK,UNLOCK,UPLOAD,DOWNLOAD,DELETE,PUSH_NEW_FILE,LIST_FILE,ACK,ACK_HELLO_CLIENT,ACK_LOCK,ACK_UNLOCK,ACK_DOWNLOAD,ACK_UPLOAD,ACK_LIST_FILE;


	public static TYPE_MSG valueOf(int i)
	{
		switch (i)
		{
			case 0 :
				return ERROR;
			case 1 : 
				return HELLO_CLIENT;
			case 2 : 
				return HELLO_SERVER;
			case 3 : 
				return LOCK;
			case 4 :
				return UNLOCK;
			case 5 :
				return UPLOAD;
			case 6:
				return DOWNLOAD;
			case 7 : 
				return DELETE;
			case 8 :
				return PUSH_NEW_FILE;
<<<<<<< HEAD
			case 9 : 
				return LIST_FILE;
			case 10:
				return ACK;
			case 11 : 
				return ACK_HELLO_CLIENT;
			case 12 :
				return ACK_LOCK;
			case 13 :
				return ACK_UNLOCK;
			case 14 :
				return ACK_DOWNLOAD;
			case 15 : 
				return ACK_UPLOAD;
			case 16 :
				return ACK_LIST_FILE;
=======
			case 9:
				return ACK;
			case 10 : 
				return ACK_HELLO_CLIENT;
			case 11 :
				return ACK_DOWNLOAD;
>>>>>>> origin/master
			default :
				return ERROR;
		}

	}
}