package ar.edu.itba.it.gossip.admin;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;

public class AdminStreamHandler implements TCPStreamHandler {

	public AdminStreamHandler(
			AdminProtocolConversation adminProtocolConversation,
			ByteStream view, OutputStream outputStream) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleEndOfInput() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleRead(ByteBuffer buf, DeferredConnector connector) {
		// TODO Auto-generated method stub

	}

}
