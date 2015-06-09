package ar.edu.itba.it.gossip.proxy.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyConfig {
	private static final ProxyConfig INSTANCE = new ProxyConfig();
	private static final String CONFIG_PATH = "/proxy.properties";

	private static final Logger log = Logger.getLogger("ProxyConfig");

	private final String adminUser;
	private final String adminPassword;

	private final String defaultOriginHost;
	private final int defaultOriginPort;
	
	private Map<String,String> userToOrigin = new HashMap<String,String>();
	private Set<String> silencedUsers = new HashSet<String>();
	private boolean convertLeet = false;
	
	private int bytesWritten = 0;

 	private ProxyConfig() {
		Properties properties = new Properties();

		InputStream is = getClass().getResourceAsStream(CONFIG_PATH);
		try {
			properties.load(is);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not load proxy properties.");
			System.exit(1);
		}

		adminUser = properties.getProperty("admin.user", "admin");
		adminPassword = properties.getProperty("admin.password", "1234");
        convertLeet = properties.getProperty("leet.default", "false").equals("true");
		defaultOriginHost = properties.getProperty("origin.default.host", "localhost");
		defaultOriginPort = Integer.parseInt(properties.getProperty("origin.default.port", "5222"));
	}
	
	public static ProxyConfig getInstance() {
		return INSTANCE;
	}
	
	//Assumes all servers have the same name as address
	public InetSocketAddress getOriginAddress(String username){
		// Assumes that all origin servers listen in port 5222 (XMPP port)
		if(userToOrigin.containsKey(username)){
				return new InetSocketAddress(userToOrigin.get(username), defaultOriginPort);
		}
		return new InetSocketAddress(defaultOriginHost, defaultOriginPort);
	}
	
	public String getOriginName(String username){
		if(userToOrigin.containsKey(username)){
			return userToOrigin.get(username);
		}
		return defaultOriginHost;
	}
	
	public void addOrigin(String username, String origin){
	    userToOrigin.put(username, origin);
	}
	public String getOriginName(){
		return defaultOriginHost;
	}
	
	public boolean convertLeet(){
	    return this.convertLeet;
	}
	
	public void setLeet(boolean convertLeet){
	    this.convertLeet = convertLeet;
	    return;
	}
	
	public void silence(String user){
	    silencedUsers.add(user.trim().toLowerCase());
	}
	
	public void unsilence(String user){
        silencedUsers.remove(user.trim().toLowerCase());
    }
	
	public boolean isSilenced(String user){
	    return silencedUsers.contains(user.trim().toLowerCase());
	}
	
	public int getStats(int type){
	    switch(type){
	        case 2:
	            return getWrittenBytes();
	    }
	    return 0;
	}
	
	public void countWrites(int written){
	    this.bytesWritten += written;
	}
	
	public int getWrittenBytes(){
	    return this.bytesWritten;
	}

	public String getAdminUser() {
		return adminUser;
	}

	public String getAdminPassword() {
		return adminPassword;
	}
}
