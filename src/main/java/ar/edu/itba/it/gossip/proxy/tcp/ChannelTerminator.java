package ar.edu.itba.it.gossip.proxy.tcp;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface ChannelTerminator {
    void closeAfterTimeout(SocketChannel channel);
}
