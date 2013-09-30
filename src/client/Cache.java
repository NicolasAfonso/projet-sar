package client;
import java.util.ArrayList;
import java.util.List;

import document.I_Document;
public class Cache {

	private List<I_Document> cache;
	
	public Cache(){
		cache = new ArrayList<>();
	}

	/**
	 * @return the cache
	 */
	public List<I_Document> getCache() {
		return cache;
	}

	/**
	 * @param cache the cache to set
	 */
	public void setCache(List<I_Document> cache) {
		this.cache = cache;
	}
	
	public void addCache(I_Document doc){
		cache.add(doc);
	}
	
	public void removeCache(I_Document doc){
		cache.remove(doc);
	}
}
