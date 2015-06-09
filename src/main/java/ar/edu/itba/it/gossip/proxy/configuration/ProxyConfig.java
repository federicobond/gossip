package ar.edu.itba.it.gossip.proxy.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyConfig {
    private static final ProxyConfig INSTANCE = new ProxyConfig();
    private static final String CONFIG_PATH = "/proxy.properties";

    private static final Logger log = Logger.getLogger("ProxyConfig");

    private String adminUser;
    private String adminPassword;

    private String defaultOriginHost;
    private int defaultOriginPort;
    private String xmppServerName;

    private Map<String, InetSocketAddress> localUsersToOrigin = new HashMap<String, InetSocketAddress>();
    private Set<String> silencedJIDs = new HashSet<String>();
    private boolean convertLeet = false;

    private AtomicLong bytesWritten = new AtomicLong();
    private AtomicLong bytesRead = new AtomicLong();
    private AtomicLong accessCount = new AtomicLong();
    private AtomicLong messagesSent = new AtomicLong();
    private AtomicLong messagesReceived = new AtomicLong();
    private AtomicLong messagesMutedOutgoing = new AtomicLong();
    private AtomicLong messagesMutedIngoing = new AtomicLong();

    private ProxyConfig() {
        Properties properties = new Properties();

        InputStream is = getClass().getResourceAsStream(CONFIG_PATH);
        try {
            properties.load(is);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not load proxy properties.");
            System.exit(1);
        }
        load(properties);
    }

    private void load(Properties properties) {
        adminUser = properties.getProperty("admin.user", "admin");
        adminPassword = properties.getProperty("admin.password", "1234");
        convertLeet = properties.getProperty("leet.default", "false").equals(
                "true");
        defaultOriginHost = properties.getProperty("origin.default.host",
                "localhost");
        defaultOriginPort = Integer.parseInt(properties.getProperty(
                "origin.default.port", "5222"));
        xmppServerName = properties
                .getProperty("xmpp.server.name", "localhost");

        String[] users = properties.getProperty("users.silenced").split(",");
        for (String user : users) {
            user = user.trim();
            if (!user.equals("")) {
                silencedJIDs.add(user);
            }
        }

        users = properties.getProperty("users.multiplexed").split(",");
        for (String user : users) {
            user = user.trim();
            if (!user.equals("")) {
                String[] parts = user.split("@");
                String[] addressParts = parts[1].split(":");
                if (parts.length != 2 || addressParts.length != 2) {
                    log.log(Level.WARNING,
                            "Skipped invalid multiplexed user value: ", user);
                    continue;
                }
                localUsersToOrigin.put(parts[0], new InetSocketAddress(
                        addressParts[0], Integer.parseInt(addressParts[1])));
            }
        }
    }

    public static ProxyConfig getInstance() {
        return INSTANCE;
    }

    public InetSocketAddress getOriginAddress(String localUsername) {
        return localUsersToOrigin.getOrDefault(localUsername,
                new InetSocketAddress(defaultOriginHost, defaultOriginPort));
    }

    public void addOriginMapping(String username, String origin) { // FIXME add
                                                                   // port as
                                                                   // parameter
        localUsersToOrigin.put(username, new InetSocketAddress(origin,
                defaultOriginPort));
    }

    public String getOriginHostname() {
        return defaultOriginHost;
    }

    public String getXMPPServerName() {
        return xmppServerName;
    }

    public boolean convertLeet() {
        return this.convertLeet;
    }

    public void setLeet(boolean convertLeet) {
        this.convertLeet = convertLeet;
        return;
    }

    public void silence(String user) {
        silencedJIDs.add(user.trim().toLowerCase());
    }

    public void unsilence(String user) {
        silencedJIDs.remove(user.trim().toLowerCase());
    }

    public boolean isJIDSilenced(String jid) {
        String[] parts = jid.split("/");
        String jidProper = parts[0]; // that is, without a resource attached
        return silencedJIDs.contains(jidProper.trim().toLowerCase());
    }

    public long getStats(int type) {
        if (type == 2) {
            return getWrittenBytes();
        }
        return 0;
    }

    public void countWrites(int written) {
        this.bytesWritten.addAndGet(written);
    }

    public long getWrittenBytes() {
        return this.bytesWritten.get();
    }

    public void countAccess() {
        this.accessCount.incrementAndGet();
    }

    public long getAccesses() {
        return this.accessCount.get();
    }

    public void countReads(int read) {
        this.bytesRead.addAndGet(read);
    }

    public long getReadBytes() {
        return this.bytesRead.get();
    }

    public void countSentMessage() {
        this.messagesSent.incrementAndGet();
    }

    public long getSentMessagesCount() {
        return this.messagesSent.get();
    }

    public void countReceivedMessage() {
        this.messagesReceived.incrementAndGet();
    }

    public long getReceivedMessagesCount() {
        return this.messagesReceived.get();
    }

    public void countMessageMutedIn() {
        this.messagesMutedIngoing.incrementAndGet();
    }

    public long getMessagesMutedIn() {
        return this.messagesMutedIngoing.get();
    }

    public void countMessageMutedOut() {
        this.messagesMutedOutgoing.incrementAndGet();
    }

    public long getMessagesMutedOut() {
        return this.messagesMutedOutgoing.get();
    }

    public void countMessage() {
        this.messagesSent.incrementAndGet();
    }

    public long getMessagesCount() {
        return this.messagesSent.get();
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getJID(String localUser) {
        return localUser + "@" + xmppServerName;
    }
}
