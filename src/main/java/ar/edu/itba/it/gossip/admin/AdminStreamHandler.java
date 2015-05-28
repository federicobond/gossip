package ar.edu.itba.it.gossip.admin;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;

public class AdminStreamHandler implements TCPStreamHandler {
    public AdminStreamHandler(AdminConversation conversation,
            InputStream fromClient, OutputStream toClient) {
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
