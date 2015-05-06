package ar.edu.itba.it.gossip.proxy.xmpp;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

public class OriginToClientXMPPStreamHandler extends XMLStreamHandler {
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private String username;
    private String password;

    public OriginToClientXMPPStreamHandler(final OutputStream toClient,
            final OutputStream toOrigin) throws XMLStreamException {
        this.toClient = toClient;
        this.toOrigin = toOrigin;

        setEventHandler(new StanzaEventHandler(this));
    }

    @Override
    public void handle(XMPPEvent event) {
    }

}
