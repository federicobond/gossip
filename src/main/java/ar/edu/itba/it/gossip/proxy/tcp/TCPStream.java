package ar.edu.itba.it.gossip.proxy.tcp;

import static ar.edu.itba.it.gossip.util.Validations.assumeNotSet;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.util.nio.ByteBufferInputStream;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

class TCPStream {
    private static final int BUF_SIZE = 4 * 1024;

    private final Endpoint from;
    private final Endpoint to;

    private TCPStreamHandler handler;// TODO: check!

    TCPStream(final SocketChannel fromChannel, final SocketChannel toChannel) {
        this.from = new Endpoint(fromChannel);
        this.to = new Endpoint(toChannel);
    }

    void setHandler(final TCPStreamHandler handler) {
        assumeNotSet(this.handler, "Handler already set: %s");
        this.handler = handler;
    }

    public TCPStreamHandler getHandler() {
        return handler;
    }

    public SocketChannel getFromChannel() {
        return from.channel;
    }

    public SocketChannel getToChannel() {
        return to.channel;
    }

    public ByteBuffer getFromBuffer() {
        return from.buffer;
    }

    public ByteBuffer getToBuffer() {
        return to.buffer;
    }

    public InputStream getInputStream() {
        return new ByteBufferInputStream(getFromBuffer());
    }

    /*
     * That is, where data would be written *into* the Stream by someone outside
     * the normal flow (i.e.: a proxy).
     * 
     * IMPORTANT: note that the data written here WILL BE THE ONLY DATA THAT
     * WILL FLOW OUTSIDE THE STREAM.
     */
    public OutputStream getOutputStream() {
        return new ByteBufferOutputStream(getToBuffer());
    }

    int getFromSubscriptionFlags() {
        return getFromBuffer().hasRemaining() ? SelectionKey.OP_READ : 0;
    }

    int getToSubscriptionFlags() {
        return getToBuffer().position() > 0 ? SelectionKey.OP_WRITE : 0;
    }

    void setToChannel(final SocketChannel channel) {
        assumeNotSet(getToChannel(), "Channel already set: %s");
        to.channel = channel;
    }

    void setFromChannel(final SocketChannel channel) {
        assumeNotSet(getFromChannel(), "Channel already set: %s");
        from.channel = channel;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    private static class Endpoint {
        SocketChannel channel;
        final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

        Endpoint(final SocketChannel channel) { // NOTE: null is acceptable
                                                // here!
            this.channel = channel;
        }

        @Override
        public String toString() {
            return reflectionToString(this);
        }
    }
}
