package ar.edu.itba.it.gossip.proxy.tcp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xmpp.ClientToOriginXMPPStreamHandler;

class TCPConversation {
    private final TCPStream clientToOrigin;
    private final TCPStream originToClient;

    TCPConversation(SocketChannel clientChannel) {
        try {
            this.clientToOrigin = new TCPStream(clientChannel, null);
            this.originToClient = new TCPStream(null, clientChannel);

            TCPStreamHandler clientHandler = new ClientToOriginXMPPStreamHandler(
                    originToClient.getOutputStream(),
                    clientToOrigin.getOutputStream());
            clientToOrigin.setHandler(clientHandler);

            // TODO: set handler for originToClient, etc!
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    void updateSubscription(Selector selector) throws ClosedChannelException {
        int clientFlags = clientToOrigin.getFromSubscriptionFlags()
                | originToClient.getToSubscriptionFlags();
        getClientChannel().register(selector, clientFlags, this);

        if (!isConnectedToOrigin()) {
            return;
        }

        final int originFlags;
        if (getOriginChannel() != null) {
            originFlags = SelectionKey.OP_CONNECT;
        } else {
            originFlags = originToClient.getFromSubscriptionFlags()
                    | clientToOrigin.getToSubscriptionFlags();
        }
        getOriginChannel().register(selector, originFlags, this);
    }

    ByteBuffer getReadBufferFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToOrigin.getFromBuffer();
        }
        if (getOriginChannel() == channel) {
            return originToClient.getFromBuffer();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    ByteBuffer getWriteBufferFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return originToClient.getToBuffer();
        }
        if (getOriginChannel() == channel) {
            return clientToOrigin.getToBuffer();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    // NOTE: A handler is activated only when data is written into its stream
    // (that is, on the TCP *read* event of the stream's input channel)
    TCPStreamHandler getHandlerFor(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToOrigin.getHandler();
        }
        if (getOriginChannel() == channel) {
            return originToClient.getHandler();
        }
        throw new IllegalArgumentException("Unknown socket");
    }

    void closeChannels() {
        try {
            try {
                getClientChannel().close();
            } finally {
                getOriginChannel().close();
            }
        } catch (IOException ignore) {
        }
    }

    SocketChannel getClientChannel() {
        return clientToOrigin.getFromChannel();
    }

    SocketChannel getOriginChannel() {
        return clientToOrigin.getToChannel();
    }

    void connectToOrigin(SocketChannel originChannel) {
        this.clientToOrigin.setToChannel(originChannel);
        this.originToClient.setFromChannel(originChannel);
        // NOTE: the rest will be handled by updateSubscription
    }

    private boolean isConnectedToOrigin() {
        return getOriginChannel() != null;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
