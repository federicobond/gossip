package ar.edu.itba.it.gossip.proxy.xmpp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.channels.SocketChannel;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.TCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;

public class XMPPConversation extends TCPConversation {

    protected XMPPConversation(SocketChannel clientChannel) {
        super(clientChannel);
        try {
            final TCPStream clientToOrigin = getClientToOriginStream();
            final TCPStream originToClient = getOriginToClientStream();

            TCPStreamHandler clientHandler = new ClientToOriginXMPPStreamHandler(
                    getOriginToClientStream().getOutputStream(),
                    getClientToOriginStream().getOutputStream());
            clientToOrigin.setHandler(clientHandler);

            // TODO: set handler for originToClient, etc!
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

}
