package ar.edu.itba.it.gossip.tcp;

import static ar.edu.itba.it.gossip.util.ExceptionUtils.unsafely;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import ar.edu.itba.it.gossip.util.Logging;

public class TCPReactorImpl implements TCPReactor, Logging {
    private static final int TIMEOUT = 3000; // Wait timeout (milliseconds)

    private final Map<Integer, TCPHandler> handlersByListenerPort;

    // Since SocketChannels don't implement hashcode and equals, compare them by
    // identity
    private final Map<SocketChannel, TCPHandler> handlersByChannel = new IdentityHashMap<>();

    private boolean running = false;
    private String hostname;

    public TCPReactorImpl(Map<Integer, TCPHandler> handlersByPort,
            String hostname) {
        this.handlersByListenerPort = handlersByPort;
        this.hostname = hostname;
    }

    @Override
    public void subscribe(SocketChannel channel, TCPHandler handler) {
        handlersByChannel.put(channel, handler);
        logInfo("Channel subscribed to handler: " + channel + "-" + handler);
    }

    @Override
    public void unsubscribe(SocketChannel channel) {
        handlersByChannel.remove(channel);
        logInfo("Done handling channel: " + channel);
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Reactor is already running!");
        }
        running = true;
        // Create a selector to multiplex listening sockets and connections
        Selector selector = Selector.open();

        // Create listening socket channel for each port and register selector
        handlersByListenerPort.forEach(unsafely(
                (port, handler) -> startListener(selector, port),
                onErrorLog("Error starting listener")));

        while (running) {
            // Wait for some channel to be ready (or timeout)
            int readyChannelCount = selector.select(TIMEOUT);
            if (readyChannelCount == 0) {
                continue;
            }
            handleEvents(selector.selectedKeys().iterator());
        }
    }

    private void startListener(Selector selector, int port) throws IOException {
        ServerSocketChannel listenerChannel = ServerSocketChannel.open();

        InetSocketAddress address = new InetSocketAddress(hostname, port);
        ServerSocket serverSocket = listenerChannel.socket();
        serverSocket.bind(address);

        // must be nonblocking to register
        listenerChannel.configureBlocking(false);
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);

        logInfo("listener started on port: " + port);
    }

    private void handleEvents(Iterator<SelectionKey> keyIter)
            throws IOException {
        while (keyIter.hasNext()) {
            SelectionKey key = keyIter.next();
            TCPHandler handler = getHandlerForChannel(key.channel());

            if (handler != null) {
                if (key.isValid() && key.isAcceptable()) {
                    handler.handleAccept(key);
                }

                if (key.isValid() && key.isConnectable()) {
                    handler.handleConnect(key);
                }

                if (key.isValid() && key.isReadable()) {
                    handler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    handler.handleWrite(key);
                }
            }
            keyIter.remove(); // remove from set of selected keys
        }
    }

    private TCPHandler getHandlerForChannel(SelectableChannel channel)
            throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) channel;
            return handlersByChannel.get(socketChannel);
        } catch (ClassCastException e) { // XXX
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
            InetSocketAddress address = (InetSocketAddress) serverSocketChannel
                    .getLocalAddress();
            int port = address.getPort();
            return handlersByListenerPort.get(port);
        }
    }
}
