package ar.edu.itba.it.gossip.admin;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.admin.tcp.UnproxiedTCPConversation;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;

public class ProxyAdmin extends TCPProxyAdmin {

    public ProxyAdmin(TCPReactor reactor) {
        super(reactor);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected UnproxiedTCPConversation instanceConversation(
            SocketChannel channel) {
        return new AdminConversation(channel);
    }

}
