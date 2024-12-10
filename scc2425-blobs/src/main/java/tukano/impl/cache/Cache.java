package tukano.impl.cache;

import java.util.logging.Logger;

import tukano.api.Result;
import utils.JSON;

public class Cache {
	
	private static Logger Log = Logger.getLogger(Cache.class.getName());

	private static final int DEFAULT_TTL = 30;

	public static <T> Result<T> getOne(String key, Class<T> clazz, Boolean refreshTimeOut) {

		var jedis = RedisCache.getCachePool().getResource();

		var value = jedis.get(key);
		
		if(value != null){
			Log.info(() -> "Value found in cache");
			
			if(refreshTimeOut)
				jedis.expire(key, DEFAULT_TTL);

			return Result.ok( JSON.decode(value, clazz));
		}

		Log.info(() -> "Value not found in cache");
        return Result.error(Result.ErrorCode.NOT_FOUND);
	}
	
	public static <T> Result<?> deleteOne(String key) {

		var jedis = RedisCache.getCachePool().getResource();
		jedis.del(key);

		return Result.ok();
	}
	
	public static <T> Result<T> updateOne(String key, T obj) {

		var jedis = RedisCache.getCachePool().getResource();

		jedis.set(key, JSON.encode(obj));

		return Result.ok(obj);
	}
	
	public static <T> Result<T> insertOne(String key, T obj) {

        var jedis = RedisCache.getCachePool().getResource();
        //var key = obj.getClass().getSimpleName() + ":" + id;
        jedis.setex(key, DEFAULT_TTL, JSON.encode(obj));
		
		return Result.ok(obj);
	}

	public static <T> Result<T> insertOne(String key, T obj, int ttl) {

        var jedis = RedisCache.getCachePool().getResource();
        jedis.setex(key, ttl, JSON.encode(obj));
		
		return Result.ok(obj);
	}
}
