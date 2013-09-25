package engine;

import java.io.Serializable;

public enum TYPE_MSG{
	ERROR,MESSAGE,ACK;


	public static TYPE_MSG valueOf(int i)
	{
		switch (i)
		{
			case 0 :
				return ERROR;
			case 1 : 
				return MESSAGE;
			case 2 : 
				return ACK;
			default :
				return ERROR;
		}

	}
}