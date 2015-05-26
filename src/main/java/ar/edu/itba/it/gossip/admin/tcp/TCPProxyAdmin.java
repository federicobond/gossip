package ar.edu.itba.it.gossip.admin.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPChannelEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.tcp.TCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;

public abstract class TCPProxyAdmin implements TCPChannelEventHandler {

	private final TCPReactor reactor;

	public TCPProxyAdmin(TCPReactor reactor) {
		this.reactor = reactor;
	}

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		ServerSocketChannel listenChannel = (ServerSocketChannel) key.channel();
		SocketChannel clientChannel = listenChannel.accept();
		clientChannel.configureBlocking(false); // Must be nonblocking to
												// register

		reactor.subscribe(clientChannel, this);
		clientChannel.register(key.selector(), SelectionKey.OP_READ
				| SelectionKey.OP_WRITE, null);

	}

	@Override
	public void handleConnect(SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleWrite(SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

	protected abstract TCPConversation instanceConversation(
			SocketChannel clientChannel);

}
