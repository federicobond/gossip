package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

public abstract class XMPPStreamHandler extends XMLStreamHandler {
    protected XMPPStreamHandler() throws XMLStreamException {
        super();
    }

    public abstract void handle(XMPPEvent event);

    protected void assumeEventType(XMPPEvent event, XMPPEvent.Type type) {
        assumeState(event.getType() == type,
                "Event type mismatch, got: %s when %s was expected", event,
                type);
    }
}
