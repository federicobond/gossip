package ar.edu.itba.it.gossip.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Proxy TCP full duplex
 * 
 * +---------+ 1 +-------+ 2 +---------------+ | cliente | --> | proxy | ---> |
 * origin server | +---------+ +-------+ +---------------+
 * 
 * 1. Por cada conexion entrante al proxy server (onAccept) 2. Intentamos
 * establecer una conexi�n al origin server (onConnect) a. Si falla la conexi�n
 * cerramos ambos sockets b. De otro modo registramos para leer algo. 3. Lo que
 * leemos desde el cliente lo escribimos en toOriginBuffer Lo que leemos desde
 * el origin server lo escribimos en toClientBuffer.
 * 
 * Si alguno de los buffers tienen algo para escribir se actualiza
 */
public class TCPProxy implements TCPHandler {
    public static final String DEFAULT_HOST = "127.0.0.1";

    private final short port;
    private final String host;

    private final TCPReactor reactor;

    public TCPProxy(TCPReactor reactor, String host, short port) {
        this.reactor = reactor;
        this.host = host;
        this.port = port;
    }

    public TCPProxy(TCPReactor reactor, short port) {
        this(reactor, DEFAULT_HOST, port);
    }

    @Override
    public void handleAccept(final SelectionKey key) throws IOException {
        final SocketChannel originServer = SocketChannel.open();

        SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
        clntChan.configureBlocking(false); // Must be nonblocking to register

        originServer.configureBlocking(false);
        // no nos registramos a ningun evento porque primero tenemos que
        // establecer la conexion hacia el origin server

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
                // nos interesa cualquier cosa que venga de cualquiera de las
                // dos puntas
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

        long bytesRead = channel.read(buffer);
        if (bytesRead == -1) { // Did the other end close?
            closeChannels(proxyState);
        } else if (bytesRead > 0) {
            proxyState.updateSubscription(key.selector());
        }
    }

    @Override
    public void handleWrite(final SelectionKey key) throws IOException {
        final ProxyState proxyState = (ProxyState) key.attachment();

        final SocketChannel channel = (SocketChannel) key.channel();

        final ByteBuffer buffer = proxyState.writeBufferFor(channel);

        buffer.flip(); // Prepare buffer for writing
        channel.write(buffer);
        buffer.compact(); // Make room for more data to be read in

        proxyState.updateSubscription(key.selector());
    }

    /**
     * Mantiene el estado del proxy. Aqui viven los dos sockets: A. El iniciado
     * por el cliente Cliente hacia el proxy server B. El iniciado por el proxy
     * server hacia el Origin Server.
     * 
     * Y sus buffers intermedios.
     * 
     * @author Juan F. Codagnone
     * @since Oct 14, 2014
     */
    private static class ProxyState {
        /**
         * crea un estado de conexion
         * 
         * @param clientChannel
         *            El iniciado por el cliente Cliente hacia el proxy server
         * @param originChannel
         *            El iniciado por el proxy server hacia el Origin Server.
         */
        ProxyState(final SocketChannel clientChannel,
                final SocketChannel originChannel) {
            if (clientChannel == null || originChannel == null) {
                throw new IllegalArgumentException();
            }
            this.clientChannel = clientChannel;
            this.originChannel = originChannel;
        }

        public final SocketChannel clientChannel;
        public final SocketChannel originChannel;

        /**
         * tama�o de buffer de lectura y escritura. Si se juega con este valor
         * se puede ver la inicidencia en CPU de un buffer chico.
         * 
         * Por ejemplo llevarlo a 1 y correr pv /path/a/un/archivo.grande | nc
         * localhost 9090
         */
        private final int BUFF_SIZE = 4 * 1024;
        public final ByteBuffer toOriginBuffer = ByteBuffer.allocate(BUFF_SIZE);
        public final ByteBuffer toClientBuffer = ByteBuffer.allocate(BUFF_SIZE);

        /** obtiene el buffer donde se deben dejar los bytes leidos */
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

        /** cierra los canales. */
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

        /**
         * Basado en el estado interno de los buffers actualiza las
         * subscripciones de eventos de los canales.
         * 
         * Basicamente: - Si hay algun byte en el buffer nos interesa escribirlo
         * - Si hay lugar para guardar un byte mas en el buffer nos interesa
         * leer
         */
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
