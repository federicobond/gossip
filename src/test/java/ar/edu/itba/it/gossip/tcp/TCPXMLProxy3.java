package ar.edu.itba.it.gossip.tcp;

import ar.edu.itba.it.gossip.util.ByteBufferOutputStream;
import ar.edu.itba.it.gossip.xmpp.ClientStreamHandler;
import ar.edu.itba.it.gossip.xmpp.StreamHandler;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class TCPXMLProxy3 implements TCPHandler {
    public static final String DEFAULT_HOST = "127.0.0.1";

    private final short port;
    private final String host;

    private final TCPReactor reactor;

    public TCPXMLProxy3(TCPReactor reactor, String host, short port) {
        this.reactor = reactor;
        this.host = host;
        this.port = port;
    }

    public TCPXMLProxy3(TCPReactor reactor, short port) {
        this(reactor, DEFAULT_HOST, port);
    }

    @Override
    public void handleAccept(final SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        clientChannel.configureBlocking(false); // Must be nonblocking to register

        /*
        final SocketChannel originServer = SocketChannel.open();
        originServer.configureBlocking(false);

        // Initiate connection to server and repeatedly poll until complete
        originServer.connect(new InetSocketAddress(host, port));
        originServer
                .register(key.selector(), SelectionKey.OP_CONNECT, clientChannel);

        */

        reactor.subscribe(clientChannel, this);

        ProxyState state = new ProxyState(clientChannel);
        state.updateSubscription(key.selector());

        /*
        reactor.subscribe(originServer, this);
        */
    }

    @Override
    public void handleConnect(final SelectionKey key) throws IOException {
        /*
        final SocketChannel clientChannel = (SocketChannel) key.attachment();

        SocketChannel originChannel = (SocketChannel) key.channel();
        final ProxyState state = new ProxyState(clientChannel, originChannel);
        try {
            boolean ret = originChannel.finishConnect();
            if (ret) {
                state.updateSubscription(key.selector());
            } else {
                closeChannels(state);
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to origin server: "
                    + e.getMessage());
            closeChannels(state);
        }
        */
    }

    private void closeChannels(ProxyState state) throws IOException {
        reactor.unsubscribe(state.clientChannel);
        /* reactor.unsubscribe(state.originChannel); */
        state.closeChannels();
    }

    @Override
    public void handleRead(final SelectionKey key) throws IOException {
        ProxyState state = (ProxyState) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (channel == state.clientChannel) {
            ByteBuffer buffer = state.readBufferFor(channel);
            StreamHandler handler = state.handlerFor(channel);

            long bytesRead = channel.read(buffer);
            if (bytesRead == -1) { // Did the other end close?
                handler.endOfInput();
                closeChannels(state);
            } else if (bytesRead > 0) {
                buffer.flip();
                handler.read(buffer);
                state.updateSubscription(key.selector());
            }
        }
    }

    @Override
    public void handleWrite(final SelectionKey key) throws IOException {
        ProxyState state = (ProxyState) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = state.writeBufferFor(channel);

        buffer.flip();
        channel.write(buffer);
        buffer.compact(); // Make room for more data to be read in

        state.updateSubscription(key.selector());
    }

    class ProxyState {

        private static final int BUF_SIZE = 4 * 1024;

        private final SocketChannel clientChannel;

        private final ByteBuffer fromClientBuffer = ByteBuffer.allocate(BUF_SIZE);
        private final ByteBuffer toClientBuffer = ByteBuffer.allocate(BUF_SIZE);

        private final StreamHandler clientHandler;

        ProxyState(SocketChannel clientChannel) {
            this.clientChannel = clientChannel;
            try {
                this.clientHandler = new ClientStreamHandler(
                    new ByteBufferOutputStream(toClientBuffer)
                );
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
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
    }
}
