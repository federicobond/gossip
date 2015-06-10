package ar.edu.itba.it.gossip.admin;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.admin.tcp.TCPProxyAdmin;
import ar.edu.itba.it.gossip.admin.tcp.UnproxiedTCPConversation;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;

public class ProxyAdmin extends TCPProxyAdmin {
    private static final long TIMEOUT = 2 * 1000; // millis

    public ProxyAdmin(TCPReactor reactor) {
        super(reactor);
    }

    @Override
    protected UnproxiedTCPConversation instanceConversation(
            SocketChannel channel) {
        return new AdminConversation(channel, c -> closeAfterTimeout(c));
    }

    protected void closeAfterTimeout(SocketChannel channel) {
        closeAfterTimeout(channel, TIMEOUT);
    }
}
