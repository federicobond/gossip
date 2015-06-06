package ar.edu.itba.it.gossip.admin;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.admin.tcp.UnproxiedTCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;

public class AdminConversation extends UnproxiedTCPConversation {
    private final TCPStream adminStream;
    
    public AdminConversation(SocketChannel clientChannel) {
        super(clientChannel);
        this.adminStream = getStream();

        final TCPStreamHandler adminHandler = new AdminStreamHandler(this,
                adminStream.getInputStream(), adminStream.getOutputStream());
        adminStream.setHandler(adminHandler);
    }

}