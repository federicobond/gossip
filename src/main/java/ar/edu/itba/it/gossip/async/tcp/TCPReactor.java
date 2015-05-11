package ar.edu.itba.it.gossip.async.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface TCPReactor {
    void start() throws IOException;

    void stop();

    void addHandler(TCPChannelEventHandler handler, int listenerPort);

    void subscribe(SocketChannel channel, TCPChannelEventHandler handler);

    void unsubscribe(SocketChannel channel);
}
