package ar.edu.itba.it.gossip.admin;

import java.nio.channels.SocketChannel;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.admin.tcp.UnproxiedTCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.ChannelTerminator;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;

public class AdminConversation extends UnproxiedTCPConversation {
    private final TCPStream adminStream;

    public AdminConversation(SocketChannel clientChannel,
            ChannelTerminator terminator) {
        super(clientChannel, terminator);
        this.adminStream = getStream();

        TCPStreamHandler adminHandler;
        try {
            adminHandler = new AdminStreamHandler(this,
                    adminStream.getInputStream(), adminStream.getOutputStream());
            adminStream.setHandler(adminHandler);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
