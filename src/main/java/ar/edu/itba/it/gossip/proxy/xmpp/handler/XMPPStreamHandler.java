package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPEventHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public abstract class XMPPStreamHandler extends XMLStreamHandler implements
        XMPPEventHandler, XMLEventHandler {

    private PartialXMPPElement xmppElement;

    protected XMPPStreamHandler() throws XMLStreamException {
        setXMLEventHandler(this);
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (xmppElement == null) {
            xmppElement = PartialXMPPElement.from(reader);
        } else {
            PartialXMPPElement newXMPPElement = PartialXMPPElement.from(reader);
            this.xmppElement.addChild(newXMPPElement);
            this.xmppElement = newXMPPElement;
        }
        handleStart(xmppElement);
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmppElement.end(reader);

        handleEnd(xmppElement);

        xmppElement = xmppElement.getParent().get(); // an element that wasn't
                                                     // open will never be
                                                     // closed, since the
                                                     // underlying stream is a
                                                     // valid XML one
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmppElement.appendToBody(reader);

        handleBody(xmppElement);
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }
}
