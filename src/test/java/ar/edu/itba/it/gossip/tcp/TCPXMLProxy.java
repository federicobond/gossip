package ar.edu.itba.it.gossip.tcp;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class TCPXMLProxy implements TCPHandler {
    public static final String DEFAULT_HOST = "127.0.0.1";

    private final short port;
    private final String host;

    private final TCPReactor reactor;

    public TCPXMLProxy(TCPReactor reactor, String host, short port) {
        this.reactor = reactor;
        this.host = host;
        this.port = port;
    }

    public TCPXMLProxy(TCPReactor reactor, short port) {
        this(reactor, DEFAULT_HOST, port);
    }

    @Override
    public void handleAccept(final SelectionKey key) throws IOException {
        final SocketChannel originServer = SocketChannel.open();

        SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
        clntChan.configureBlocking(false); // Must be nonblocking to register

        originServer.configureBlocking(false);

        // Initiate connection to server and repeatedly poll until complete
        originServer.connect(new InetSocketAddress(host, port));
        originServer
                .register(key.selector(), SelectionKey.OP_CONNECT, clntChan);

        reactor.subscribe(clntChan, this);
        reactor.subscribe(originServer, this);
    }

    @Override
    public void handleConnect(final SelectionKey key) throws IOException {
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
    }

    private void closeChannels(ProxyState state) throws IOException {
        reactor.unsubscribe(state.clientChannel);
        reactor.unsubscribe(state.originChannel);
        state.closeChannels();
    }

    @Override
    public void handleRead(final SelectionKey key) throws IOException {
        final ProxyState proxyState = (ProxyState) key.attachment();
        final SocketChannel channel = (SocketChannel) key.channel();

        final ByteBuffer buffer = proxyState.readBufferFor(channel);
        final AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = proxyState.parserFor(channel);

        long bytesRead = channel.read(buffer);

        if (bytesRead == -1) { // Did the other end close?
            closeChannels(proxyState);
            parser.getInputFeeder().endOfInput();
        } else if (bytesRead > 0) {
            int pos = 0;
            try {
                pos = buffer.position();
                buffer.flip();
                parser.getInputFeeder().feedInput(buffer);
                int type = 0;
                while (true) {
                    type = parser.next();
                    if (type == AsyncXMLStreamReader.EVENT_INCOMPLETE || type == AsyncXMLStreamReader.END_DOCUMENT) {
                        break;
                    }
                    System.out.println(type);
                }
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            } finally {
                buffer.position(pos);
                buffer.limit(buffer.capacity());
            }
            proxyState.updateSubscription(key.selector());
        }
    }

    @Override
    public void handleWrite(final SelectionKey key) throws IOException {
        final ProxyState proxyState = (ProxyState) key.attachment();

        final SocketChannel channel = (SocketChannel) key.channel();

        final ByteBuffer buffer = proxyState.writeBufferFor(channel);

        buffer.flip();
        channel.write(buffer);
        buffer.compact(); // Make room for more data to be read in

        proxyState.updateSubscription(key.selector());
    }

    private static class ProxyState {

        public final SocketChannel clientChannel;
        public final SocketChannel originChannel;

        private final int BUFF_SIZE = 4 * 1024;

        public final ByteBuffer toOriginBuffer = ByteBuffer.allocate(BUFF_SIZE);
        public final ByteBuffer toClientBuffer = ByteBuffer.allocate(BUFF_SIZE);

        public final AsyncXMLStreamReader<AsyncByteBufferFeeder> clientParser;
        public final AsyncXMLStreamReader<AsyncByteBufferFeeder> originParser;


        ProxyState(final SocketChannel clientChannel, final SocketChannel originChannel) {
            AsyncXMLInputFactory xmlInputFactory = new InputFactoryImpl();
            try {
                clientParser = xmlInputFactory.createAsyncFor(ByteBuffer.allocate(0));
                originParser = xmlInputFactory.createAsyncFor(ByteBuffer.allocate(0));
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }

            if (clientChannel == null || originChannel == null) {
                throw new IllegalArgumentException();
            }
            this.clientChannel = clientChannel;
            this.originChannel = originChannel;
        }

        public ByteBuffer readBufferFor(final SocketChannel channel) {
            final ByteBuffer ret;
            // no usamos equals porque la comparacion es suficiente por
            // instancia
            if (clientChannel == channel) {
                ret = toOriginBuffer;
            } else if (originChannel == channel) {
                ret = toClientBuffer;
            } else {
                throw new IllegalArgumentException("socket desconocido");
            }

            return ret;
        }

        public ByteBuffer writeBufferFor(SocketChannel channel) {
            final ByteBuffer ret;

            // no usamos equals porque la comparacion es suficiente por
            // instancia
            if (clientChannel == channel) {
                ret = toClientBuffer;
            } else if (originChannel == channel) {
                ret = toOriginBuffer;
            } else {
                throw new IllegalArgumentException("socket desconocido");
            }

            return ret;
        }

        public AsyncXMLStreamReader<AsyncByteBufferFeeder> parserFor(SocketChannel channel) {
            if (clientChannel == channel) {
                return clientParser;
            } else if (originChannel == channel) {
                return originParser;
            } else {
                throw new IllegalArgumentException("socket desconocido");
            }
        }

        public void closeChannels() throws IOException {
            // el try finally es importante porque el primer close podria tirar
            // una
            // IOException y eso cortar el flujo dejando abierto el segundo
            // socket
            try {
                clientChannel.close();
            } finally {
                originChannel.close();
            }
        }

        public void updateSubscription(Selector selector)
                throws ClosedChannelException {
            int originFlags = 0;
            int clientFlags = 0;

            if (toOriginBuffer.hasRemaining()) {
                clientFlags |= SelectionKey.OP_READ;
            }
            if (toClientBuffer.hasRemaining()) {
                originFlags |= SelectionKey.OP_READ;
            }

            if (toOriginBuffer.position() > 0) {
                originFlags |= SelectionKey.OP_WRITE;
            }
            if (toClientBuffer.position() > 0) {
                clientFlags |= SelectionKey.OP_WRITE;
            }

            clientChannel.register(selector, clientFlags, this);
            originChannel.register(selector, originFlags, this);
        }
    }
}
