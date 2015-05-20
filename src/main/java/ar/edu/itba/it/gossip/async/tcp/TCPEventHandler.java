package ar.edu.itba.it.gossip.async.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface TCPEventHandler {
    void handleAccept(SelectionKey key) throws IOException;

    void handleConnect(SelectionKey key) throws IOException;

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;
}
