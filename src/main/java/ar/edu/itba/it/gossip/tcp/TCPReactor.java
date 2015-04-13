package ar.edu.itba.it.gossip.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface TCPReactor {
    void start() throws IOException;

    void stop();

    void subscribe(SocketChannel channel, TCPHandler handler);

    void unsubscribe(SocketChannel channel);
}
