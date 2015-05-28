package ar.edu.itba.it.gossip.admin;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.proxy.tcp.TCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;

public class AdminProtocolConversation extends AdminTCPConversation {

	public AdminProtocolConversation(SocketChannel clientChannel) {
		super(clientChannel);
		final TCPStream adminStream = getStream();

		final TCPStreamHandler adminHandler = new AdminStreamHandler(this,
				adminStream.getView(), adminStream.getOutputStream());
		adminStream.setHandler(adminHandler);
	}
	
}
