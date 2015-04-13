package ar.edu.itba.it.gossip.tcp;

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

import org.apache.log4j.Logger;

public class TCPReactorImpl implements TCPReactor {
	private static final int TIMEOUT = 3000; // Wait timeout (milliseconds)
	private static final Logger LOGGER = Logger.getLogger(TCPReactorImpl.class);

	private final Map<Integer, TCPHandler> handlersByPort;

	// Since SocketChannels don't implement hashcode and equals, compare them by
	// identity
	private final Map<SocketChannel, TCPHandler> handlersByChannel = new IdentityHashMap<>();

	private boolean running = false;
	private String hostname;

	public TCPReactorImpl(Map<Integer, TCPHandler> handlersByPort,
			String hostname) {
		this.handlersByPort = handlersByPort;
		this.hostname = hostname;
	}

	@Override
	public void subscribe(SocketChannel channel, TCPHandler handler) {
		handlersByChannel.put(channel, handler);
		LOGGER.info("Channel subscribed to handler: " + channel + "-" + handler);
	}

	@Override
	public void unsubscribe(SocketChannel channel) {
		handlersByChannel.remove(channel);
		LOGGER.info("Done handling channel: " + channel);
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
		handlersByPort
				.forEach((port, handler) -> startListener(selector, port));

		while (running) {
			// Wait for some channel to be ready (or timeout)
			if (selector.select(TIMEOUT) == 0) { // returns # of ready chans
				continue;
			}
			handleEvents(selector.selectedKeys().iterator());
		}
	}

	private void startListener(Selector selector, int port) {
		try {
			ServerSocketChannel listenChannel = ServerSocketChannel.open();

			InetSocketAddress address = new InetSocketAddress(hostname, port);
			ServerSocket serverSocket = listenChannel.socket();
			serverSocket.bind(address);

			// must be nonblocking to register
			listenChannel.configureBlocking(false);
			listenChannel.register(selector, SelectionKey.OP_ACCEPT);

			LOGGER.info("listener started on port: " + port);
		} catch (Exception ex) {
			LOGGER.error("error starting listener on port: " + port, ex);
			throw new RuntimeException(ex); // XXX
		}
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

	private TCPHandler getHandlerForChannel(SelectableChannel channel) throws IOException {
		try {
			SocketChannel socketChannel = (SocketChannel) channel;
			return handlersByChannel.get(socketChannel);
		} catch (ClassCastException e) { // XXX
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
			InetSocketAddress address = (InetSocketAddress) serverSocketChannel
					.getLocalAddress();
			int port = address.getPort();
			return handlersByPort.get(port);
		}
	}
}