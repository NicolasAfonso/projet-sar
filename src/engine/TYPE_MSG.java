package engine;

import java.io.Serializable;

public enum TYPE_MSG{
	ERROR,HELLO_CLIENT,HELLO_SERVER,LOCK,UNLOCK,UPLOAD,DOWNLOAD,DELETE,ACK, PUSH_NEW_FILE;


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
				return ACK;
			case 9:
				return PUSH_NEW_FILE;
			default :
				return ERROR;
		}

	}
}