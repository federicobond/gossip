package ar.edu.itba.it.gossip.proxy.xmpp;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeNotSet;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.channels.SocketChannel;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.TCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler;

public class XMPPConversation extends TCPConversation {
    private Credentials credentials;

    protected XMPPConversation(SocketChannel clientChannel) {
        super(clientChannel);
        try {
            final TCPStream clientToOrigin = getClientToOriginStream();
            final TCPStream originToClient = getOriginToClientStream();

            final TCPStreamHandler clientToOriginHandler = new ClientToOriginXMPPStreamHandler(
                    this, clientToOrigin.getView(),
                    originToClient.getOutputStream());
            clientToOrigin.setHandler(clientToOriginHandler);

            final TCPStreamHandler originToClientHandler = new OriginToClientXMPPStreamHandler(
                    this, originToClient.getView(),
                    clientToOrigin.getOutputStream());
            originToClient.setHandler(originToClientHandler);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCredentials(final Credentials credentials) {
        assumeNotSet(this.credentials, "Credentials already set %s",
                this.credentials);
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
