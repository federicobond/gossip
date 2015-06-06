package ar.edu.itba.it.gossip.proxy.xmpp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.tcp.TCPProxy;

public class XMPPProxy extends TCPProxy {
    public XMPPProxy(TCPReactor reactor) {
        super(reactor);
    }

    @Override
    protected XMPPConversation instanceConversation(SocketChannel clientChannel) {
        return new XMPPConversation(clientChannel);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
