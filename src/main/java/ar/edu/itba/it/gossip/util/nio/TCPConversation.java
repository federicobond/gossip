package ar.edu.itba.it.gossip.util.nio;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;

public interface TCPConversation {
    void updateSubscription(Selector selector) throws ClosedChannelException;

    void closeChannels();
}
