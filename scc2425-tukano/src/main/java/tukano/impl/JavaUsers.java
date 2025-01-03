package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.DB;
import tukano.impl.cache.MyCache;


public class JavaUsers implements Users {
	
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private static Users instance;
	
	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}
	
	private JavaUsers() {
		createUser(new User("admin", "admin", "", "admin"));
	}
	
	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo( user ) )
			return error(BAD_REQUEST);

		Result<User> res;

		Log.info(() -> "Using SQL DB");
		res = DB.insertOne(user);

		if(res.isOK()){
			Log.info(() -> "Inserting into cache");
			MyCache.insertOne("users:"+user.getUserId(), user);
		}
		Log.info(() -> "Returning result");
		return errorOrValue(res, user.getUserId());
	}

	@Override
	public Result<Response> getUser(String userId, String pwd) {
		Log.info( () -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null)
			return error(BAD_REQUEST);
		
		var res = MyCache.getOne("users:"+userId, User.class, true);
		if(res.isOK()){

			Log.info(() -> "User found in cache ");
			
			NewCookie cookie = Authentication.login(res.value().getUserId(), pwd);
			var result = validatedUserOrError(res, pwd);
			if(!result.isOK()){
				return error(result.error());
			}
			return ok(Response.ok(result.value()).cookie(cookie).build());
		}

		Result<User> dbres = DB.getOne( userId, User.class);

		if(dbres.isOK()){
			Log.info(() -> "User found in DB");
			MyCache.insertOne("users:"+userId, dbres.value());
		}

		NewCookie cookie = Authentication.login(dbres.value().getUserId(), pwd);
		var result = validatedUserOrError(dbres, pwd);
		if(!result.isOK()){
			return error(result.error());
		}
		return ok(Response.ok(result.value()).cookie(cookie).build());
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
				return error(BAD_REQUEST);

		return errorOrResult( validatedUserOrError(DB.getOne(userId, User.class), pwd), user -> {
			var res = DB.updateOne( user.updateFrom(other));
			if(res.isOK()){
				MyCache.updateOne("users:"+userId, res.value());
			}
			return res;
		});
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null )
			return error(BAD_REQUEST);

		return errorOrResult( validatedUserOrError(DB.getOne( userId, User.class), pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread( () -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
			}).start();

			MyCache.deleteOne("users:"+userId);
			
			return DB.deleteOne( user);
		});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info( () -> format("searchUsers : patterns = %s\n", pattern));

		var query = format("SELECT * FROM users u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = DB.sql(query, User.class)
				.stream()
				.map(User::copyWithoutPassword)
				.toList();

		return ok(hits);
	}

	
	private Result<User> validatedUserOrError( Result<User> res, String pwd ) {
		if( res.isOK())
			return res.value().getPwd().equals( pwd ) ? res : error(FORBIDDEN);
		else
			return res;
	}
	
	private boolean badUserInfo( User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}
	
	private boolean badUpdateUserInfo( String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && ! userId.equals( info.getUserId()));
	}
}