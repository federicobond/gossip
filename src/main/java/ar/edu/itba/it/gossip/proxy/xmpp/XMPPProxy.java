package ar.edu.itba.it.gossip.proxy.xmpp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.tcp.TCPProxy;

public class XMPPProxy extends TCPProxy {
    private static final long TIMEOUT = 2 * 1000; // millis

    public XMPPProxy(TCPReactor reactor) {
        super(reactor);
    }

    @Override
    protected ProxiedXMPPConversation instanceConversation(
            SocketChannel clientChannel) {
        return new ProxiedXMPPConversation(clientChannel,
                channel -> closeAfterTimeout(channel));
    }

    protected void closeAfterTimeout(SocketChannel channel) {
        closeAfterTimeout(channel, TIMEOUT);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
