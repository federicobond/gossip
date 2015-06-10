package ar.edu.itba.it.gossip.async.tcp;

import static ar.edu.itba.it.gossip.util.CollectionUtils.removeAll;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import static ar.edu.itba.it.gossip.util.nio.ChannelUtils.closeQuietly;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPReactorImpl implements TCPReactor {
    private static final int TIMEOUT = 3000; // Wait timeout (milliseconds)

    private final Logger logger = LoggerFactory.getLogger(TCPReactorImpl.class);

    private final Map<Integer, TCPEventHandler> handlersByListenerPort = new HashMap<>();

    // Since SocketChannels don't implement hashcode and equals, compare them by
    // identity
    private final Map<SocketChannel, TCPEventHandler> handlersByChannel = new IdentityHashMap<>();
    private final Map<SocketChannel, Long> timeoutTimesByChannel = new IdentityHashMap<>();

    private boolean running = false;

    public TCPReactorImpl() {
    }

    @Override
    public void addHandler(TCPEventHandler handler, int listenerPort) {
        this.handlersByListenerPort.put(listenerPort, handler);
    }

    @Override
    public void subscribe(SocketChannel channel, TCPEventHandler handler) {
        // TODO: shouldn't we be checking for collisions here?
        handlersByChannel.put(channel, handler);
        logger.info("Subscribed channel {} to handler {}", channel, handler);
    }

    @Override
    public void unsubscribe(SocketChannel channel) {
        handlersByChannel.remove(channel);
        timeoutTimesByChannel.remove(channel);
        if (channel != null) {
            logger.info("Done handling channel: {}", channel);
        }
    }

    @Override
    public void stop() {
        this.running = false;
        logger.info("[STOP]: {}", this);
    }

    @Override
    public void closeAfterTimeout(SocketChannel channel, long millis) {
        assumeState(!timeoutTimesByChannel.containsKey(channel),
                "%s is already on timeout", channel);
        long timeoutTime = currentTimeMillis() + millis;
        timeoutTimesByChannel.put(channel, timeoutTime);
    }

    @Override
    public void start() throws IOException {
        assumeState(!running, "%s is already running", this);
        assumeState(!handlersByListenerPort.isEmpty(),
                "A non-empty Map of handlers is expected");

        running = true;
        // Create a selector to multiplex listening sockets and connections
        Selector selector = Selector.open();

        // Create listening socket channel for each port and register selector
        for (Integer port : handlersByListenerPort.keySet()) {
            startListener(selector, port);
        }

        logger.info("[START]: {}", this);
        while (running) {
            try {
                checkTimeouts();
                // Wait for some channel to be ready (or timeout)
                int readyChannelCount = selector.select(TIMEOUT);
                if (readyChannelCount == 0) {
                    continue;
                }
                handleEvents(selector.selectedKeys().iterator());
            } catch (Exception ex) {
                logger.error("{} fail", this);
            }
        }
    }

    private void startListener(Selector selector, int port) throws IOException {
        ServerSocketChannel listenerChannel = ServerSocketChannel.open();

        InetSocketAddress address = new InetSocketAddress("localhost", port);
        ServerSocket serverSocket = listenerChannel.socket();
        serverSocket.bind(address);

        // must be nonblocking to register
        listenerChannel.configureBlocking(false);
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);

        TCPEventHandler handler = handlersByListenerPort.get(port);
        logger.info("Subscribed handler {} as listener on port {}", handler,
                port);
    }

    private void handleEvents(Iterator<SelectionKey> keyIter)
            throws IOException {
        while (keyIter.hasNext()) {
            SelectionKey key = keyIter.next();
            TCPEventHandler handler = getHandlerForChannel(key.channel());

            if (handler != null) {
                if (key.isValid() && key.isAcceptable()) {
                    logger.info("Handling TCP accept with handler: {}", handler);
                    handler.handleAccept(key);
                }

                if (key.isValid() && key.isConnectable()) {
                    logger.info("Handling TCP connect with handler: {}",
                            handler);
                    handler.handleConnect(key);
                }

                if (key.isValid() && key.isReadable()) {
                    logger.info("Handling TCP read with handler: {}", handler);
                    handler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    logger.info("Handling TCP write with handler: {}", handler);
                    handler.handleWrite(key);
                }
            } else {
                logger.info("No handler found for channel: {}", key.channel());
            }
            keyIter.remove(); // remove from set of selected keys
        }
    }

    private TCPEventHandler getHandlerForChannel(SelectableChannel channel)
            throws IOException {
        if (channel instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) channel;
            return handlersByChannel.get(socketChannel);
        }
        if (channel instanceof ServerSocketChannel) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
            InetSocketAddress address = (InetSocketAddress) serverSocketChannel
                    .getLocalAddress();
            int port = address.getPort();
            return handlersByListenerPort.get(port);
        }
        throw new IllegalArgumentException("Unknown channel type: " + channel);
    }

    private void checkTimeouts() {
        long time = currentTimeMillis();

        List<SocketChannel> toRemove = new LinkedList<>();
        for (Entry<SocketChannel, Long> entry : timeoutTimesByChannel
                .entrySet()) {
            SocketChannel channel = entry.getKey();
            long timeoutTime = entry.getValue();
            if (time >= timeoutTime) {
                closeQuietly(channel);
                unsubscribe(channel);
                toRemove.add(channel);
            }
        }
        removeAll(timeoutTimesByChannel, toRemove);
    }
}
