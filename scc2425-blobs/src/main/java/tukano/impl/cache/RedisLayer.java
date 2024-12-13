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
		MyCache.insertOne(s.uid(), s,3600);
	}
	
	public Session getSession(String uid) {
		return MyCache.getOne(uid, Session.class, false).value();
	}
}
