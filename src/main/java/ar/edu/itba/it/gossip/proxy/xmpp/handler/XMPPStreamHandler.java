package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPEventHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

public abstract class XMPPStreamHandler extends XMLStreamHandler implements
        XMPPEventHandler {
    protected XMPPStreamHandler() throws XMLStreamException {
        super();
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }
}
