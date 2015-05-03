package ar.edu.itba.it.gossip.proxy.tcp;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface DeferredConnector {
    void connectToOrigin(InetSocketAddress address);
}
