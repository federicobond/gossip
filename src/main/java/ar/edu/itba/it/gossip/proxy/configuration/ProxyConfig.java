package ar.edu.itba.it.gossip.proxy.configuration;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ProxyConfig {
    private static final ProxyConfig INSTANCE = new ProxyConfig();

    // For now, you just can change the origin here
    private final String DEFAULT_ORIGIN_ADDRESS = "localhost";
    private final int DEFAULT_ORIGIN_PORT = 5222;

    private Map<String, String> userToOrigin = new HashMap<String, String>();

    private ProxyConfig() {
    }

    public static ProxyConfig getInstance() {
        return INSTANCE;
    }

    public InetSocketAddress getOriginAddress(String username) {
        // Assumes that all origin servers listen in port 5222 (XMPP port)
        if (userToOrigin.containsKey(username)) {
            return new InetSocketAddress(userToOrigin.get(username),
                    DEFAULT_ORIGIN_PORT);
        }
        return new InetSocketAddress(DEFAULT_ORIGIN_ADDRESS,
                DEFAULT_ORIGIN_PORT);
    }

    public String getOriginName(String username) {
        if (userToOrigin.containsKey(username)) {
            return userToOrigin.get(username);
        }
        return DEFAULT_ORIGIN_ADDRESS;
    }

    public String getOriginName() {
        return DEFAULT_ORIGIN_ADDRESS;
    }

}
