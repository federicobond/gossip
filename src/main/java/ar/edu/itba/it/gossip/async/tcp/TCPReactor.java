package ar.edu.itba.it.gossip.async.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface TCPReactor {
    void start() throws IOException;

    void stop();

    void addHandler(TCPEventHandler handler, int listenerPort);

    void subscribe(SocketChannel channel, TCPEventHandler handler);

    void unsubscribe(SocketChannel channel);

    void closeAfterTimeout(SocketChannel channel, long millis);
}
