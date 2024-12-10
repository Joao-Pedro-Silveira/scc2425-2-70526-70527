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
		Cache.insertOne(s.uid(), s,3600);
	}
	
	public Session getSession(String uid) {
		return Cache.getOne(uid, Session.class, false).value();
	}
}
