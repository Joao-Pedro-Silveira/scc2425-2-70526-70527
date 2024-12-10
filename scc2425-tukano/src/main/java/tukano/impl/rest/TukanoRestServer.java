package tukano.impl.rest;

//import java.net.URI;
import java.util.logging.Logger;

//import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import tukano.impl.Authentication;
import tukano.impl.Token;
import tukano.impl.auth.RequestCookiesCleanupFilter;
import tukano.impl.auth.RequestCookiesFilter;
import utils.Args;
import utils.IP;


public class TukanoRestServer extends Application {
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	static final String INETADDR_ANY = "0.0.0.0";
	static String SERVER_BASE_URI = "http://%s:%s/rest";

	public static final int PORT = 8080;

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	public TukanoRestServer() {
		Token.setSecret( Args.valueOf("-secret", "bonk"));
		singletons.add(new ResourceConfig());
		resources.add(RestUsersResource.class);
		resources.add(RestShortsResource.class);
		resources.add(RequestCookiesFilter.class);
     	resources.add(RequestCookiesCleanupFilter.class);
        resources.add(Authentication.class);
	}
	
	public static void main(String[] args) throws Exception {
		Args.use(args);
		
		Token.setSecret( Args.valueOf("-secret", "bonk"));
//		Props.load( Args.valueOf("-props", "").split(","));
		
		new TukanoRestServer();
	}
	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
