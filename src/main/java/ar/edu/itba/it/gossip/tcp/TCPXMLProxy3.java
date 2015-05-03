package ar.edu.itba.it.gossip.tcp;

import ar.edu.itba.it.gossip.xmpp.StreamHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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

    public TCPReactor getReactor() {
        return reactor;
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
        ProxyState state = (ProxyState) key.attachment();
        state.updateSubscription(key.selector());

        /*
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
        reactor.unsubscribe(state.getClientChannel());
        /* reactor.unsubscribe(state.originChannel); */
        state.closeChannels();
    }

    @Override
    public void handleRead(final SelectionKey key) throws IOException {
        ProxyState state = (ProxyState) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (channel == state.getClientChannel()) {
            ByteBuffer buffer = state.readBufferFor(channel);
            StreamHandler handler = state.handlerFor(channel);

            long bytesRead = channel.read(buffer);
            if (bytesRead == -1) { // Did the other end close?
                handler.endOfInput();
                closeChannels(state);
            } else if (bytesRead > 0) {
                buffer.flip();
                handler.read(buffer, new Connector(key, state));

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

    public class Connector {
        private final SelectionKey key;
        private final ProxyState state;

        public Connector(SelectionKey key, ProxyState state) {
            this.key = key;
            this.state = state;
        }

        public void connect(InetSocketAddress address) {
            try {
                SocketChannel origin = SocketChannel.open();
                origin.configureBlocking(false);
                origin.connect(address);

                state.setOriginChannel(origin);
                origin.register(key.selector(), SelectionKey.OP_CONNECT, state);
                TCPXMLProxy3.this.getReactor().subscribe(origin, TCPXMLProxy3.this);

            } catch (IOException e) {
                throw new RuntimeException("should inform this to client");
            }
        }

    }
}
