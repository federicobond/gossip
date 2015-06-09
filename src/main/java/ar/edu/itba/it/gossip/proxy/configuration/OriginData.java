package ar.edu.itba.it.gossip.proxy.configuration;

public class OriginData {
    private final String hostname;
    private final int port;
    private final String serverName;

    OriginData(final String hostname, final int port, final String serverName) {
        this.hostname = hostname;
        this.port = port;
        this.serverName = serverName;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getServerName() {
        return serverName;
    }
}
