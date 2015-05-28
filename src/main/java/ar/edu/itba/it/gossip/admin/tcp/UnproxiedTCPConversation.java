package ar.edu.itba.it.gossip.admin.tcp;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;
import ar.edu.itba.it.gossip.util.nio.TCPConversation;

public class UnproxiedTCPConversation implements TCPConversation {
    private final TCPStream stream;

    protected UnproxiedTCPConversation(SocketChannel channel) {
        this.stream = new TCPStream(channel, channel);
    }

    @Override
    public void updateSubscription(Selector selector)
            throws ClosedChannelException {
        int streamFlags = stream.getFromSubscriptionFlags();
        stream.getFromChannel().register(selector, streamFlags, this);
    }

    @Override
    public void closeChannels() {
        try {
            stream.getFromChannel().close();
        } catch (IOException ignore) {
        }

    }

    public TCPStream getStream() {
        return stream;
    }
}
