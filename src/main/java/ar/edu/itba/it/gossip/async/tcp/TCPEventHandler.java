package ar.edu.itba.it.gossip.async.tcp;

import java.nio.channels.SelectionKey;

public interface TCPEventHandler {
    void handleAccept(SelectionKey key);

    void handleConnect(SelectionKey key);

    void handleRead(SelectionKey key);

    void handleWrite(SelectionKey key);
}
