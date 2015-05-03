package ar.edu.itba.it.gossip.tcp;

import ar.edu.itba.it.gossip.util.ByteBufferOutputStream;
import ar.edu.itba.it.gossip.xmpp.ClientStreamHandler;
import ar.edu.itba.it.gossip.xmpp.StreamHandler;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProxyState {

    private static final int BUF_SIZE = 4 * 1024;

    private final SocketChannel clientChannel;
    private SocketChannel originChannel;

    private final ByteBuffer fromClientBuffer = ByteBuffer.allocate(BUF_SIZE);
    private final ByteBuffer toClientBuffer = ByteBuffer.allocate(BUF_SIZE);

    private final StreamHandler clientHandler;

    ProxyState(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
        try {
            this.clientHandler = new ClientStreamHandler(
                    this,
                    new ByteBufferOutputStream(toClientBuffer)
            );
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public void updateSubscription(Selector selector) throws ClosedChannelException {
        int clientFlags = 0;

        if (fromClientBuffer.hasRemaining()) {
            clientFlags |= SelectionKey.OP_READ;
        }

        if (toClientBuffer.position() > 0) {
            clientFlags |= SelectionKey.OP_WRITE;
        }

        clientChannel.register(selector, clientFlags, this);
    }

    public ByteBuffer readBufferFor(final SocketChannel channel) {
        return fromClientBuffer;
    }

    public ByteBuffer writeBufferFor(SocketChannel channel) {
        return toClientBuffer;
    }

    public StreamHandler handlerFor(SocketChannel channel) {
        return clientHandler;
    }

    public void closeChannels() {
        try {
            clientChannel.close();
        } catch (IOException ignore) {}
    }

    public void setOriginChannel(SocketChannel originChannel) {
        this.originChannel = originChannel;
    }
}
