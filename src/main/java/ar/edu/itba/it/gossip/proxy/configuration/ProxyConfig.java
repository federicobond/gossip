package ar.edu.itba.it.gossip.proxy.configuration;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProxyConfig {
	private static final ProxyConfig INSTANCE = new ProxyConfig();
	
	// For now, you just can change the origin here
	private final String DEFAULT_ORIGIN_ADDRESS = "localhost";
	private final int DEFAULT_ORIGIN_PORT = 5222;
	
	private Map<String,String> userToOrigin = new HashMap<String,String>();
	private Set<String> silencedUsers = new HashSet<String>();
	private boolean convertLeet = false;
	
 	private ProxyConfig() {}
	
	public static ProxyConfig getInstance() {
		return INSTANCE;
	}
	
	//Assumes all servers have the same name as address
	public InetSocketAddress getOriginAddress(String username){
		// Assumes that all origin servers listen in port 5222 (XMPP port)
		if(userToOrigin.containsKey(username)){
				return new InetSocketAddress(userToOrigin.get(username),DEFAULT_ORIGIN_PORT);
		}
		return new InetSocketAddress(DEFAULT_ORIGIN_ADDRESS,DEFAULT_ORIGIN_PORT);
	}
	
	public String getOriginName(String username){
		if(userToOrigin.containsKey(username)){
			return userToOrigin.get(username);
		}
		return DEFAULT_ORIGIN_ADDRESS;
	}
	
	public void addOrigin(String username, String origin){
	    userToOrigin.put(username, origin);
	}
	public String getOriginName(){
		return DEFAULT_ORIGIN_ADDRESS;
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
	
	public boolean isSilenced(String user){
	    return silencedUsers.contains(user.trim().toLowerCase());
	}
}
