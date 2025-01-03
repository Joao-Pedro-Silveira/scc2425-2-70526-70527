package tukano.impl;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tukano.impl.auth.RequestCookies;

import tukano.impl.cache.RedisLayer;

public class Authentication {
	static final String COOKIE_KEY = "scc:session";
	static final String LOGIN_PAGE = "login.html";
	private static final int MAX_COOKIE_AGE = 3600;

	static public Response login( String userId, String password ) {
		System.out.println("user: " + userId + " pwd:" + password );
		boolean pwdOk = true;
		if (pwdOk) {
			String uid = UUID.randomUUID().toString();
			var cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(uid).path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) //ideally it should be true to only work for https requests
					.httpOnly(true)
					.build();
			
			RedisLayer.getInstance().putSession( new Session( uid, userId));	
			
            return Response.ok()
                    .cookie(cookie) 
                    .build();
		} else
			throw new NotAuthorizedException("Incorrect login");
	}
	
	public String login() {
		try {
			var in = getClass().getClassLoader().getResourceAsStream(LOGIN_PAGE);
			return new String( in.readAllBytes() );	
		} catch( Exception x ) {
			throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
		}
	}
	
	static public Session validateSession(String userId) throws NotAuthorizedException {
		var cookies = RequestCookies.get();
		return validateSession( cookies.get(COOKIE_KEY ), userId );
	}
	
	static public Session validateSession(Cookie cookie, String userId) throws NotAuthorizedException {

		if (cookie == null )
			throw new NotAuthorizedException("No session initialized");
		
		var session = RedisLayer.getInstance().getSession( cookie.getValue());
		if( session == null )
			throw new NotAuthorizedException("No valid session initialized");
			
		if (session.user() == null || session.user().length() == 0) 
			throw new NotAuthorizedException("No valid session initialized");
		
		if (!session.user().equals(userId))
			throw new NotAuthorizedException("Invalid user : " + session.user());
		
		return session;
	}

	static public Session validateSession() throws NotAuthorizedException {
		var cookies = RequestCookies.get();
		var cookie = cookies.get(COOKIE_KEY);
		if (cookie == null )
			throw new NotAuthorizedException("No session initialized");
		
		var session = RedisLayer.getInstance().getSession( cookie.getValue());
		if( session == null )
			throw new NotAuthorizedException("No valid session initialized");
			
		if (session.user() == null || session.user().length() == 0) 
			throw new NotAuthorizedException("No valid session initialized");
		
		
		return session;
	}
}
