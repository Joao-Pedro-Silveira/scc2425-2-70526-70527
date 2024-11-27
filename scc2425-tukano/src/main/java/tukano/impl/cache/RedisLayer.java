package tukano.impl.cache;

import tukano.impl.Session;

public class RedisLayer {

	private static RedisLayer instance;
	synchronized public static RedisLayer getInstance() {
		if(instance == null )
			instance = new RedisLayer();
		return instance;
	}
	
	
	public void putSession(Session s) {
		CacheForCosmos.insertOne(s.uid(), s);
	}
	
	public Session getSession(String uid) {
		return CacheForCosmos.getOne(uid, Session.class, true).value();
	}
}
