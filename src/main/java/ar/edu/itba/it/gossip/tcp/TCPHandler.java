package ar.edu.itba.it.gossip.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface TCPHandler {
	void handleAccept(SelectionKey key) throws IOException;

	void handleConnect(SelectionKey key) throws IOException;

	void handleRead(SelectionKey key) throws IOException;

	void handleWrite(SelectionKey key) throws IOException;
}