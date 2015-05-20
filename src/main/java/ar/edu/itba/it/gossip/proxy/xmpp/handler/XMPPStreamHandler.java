package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPEventHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public abstract class XMPPStreamHandler extends XMLStreamHandler implements
        XMPPEventHandler, XMLEventHandler {

    private PartialXMLElement xmlElement;

    protected XMPPStreamHandler() throws XMLStreamException {
        setXMLEventHandler(this);
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (xmlElement == null) {
            xmlElement = new PartialXMLElement();
        } else {
            xmlElement = new PartialXMLElement(xmlElement);
        }
        xmlElement.loadName(reader).loadAttributes(reader);

        handleStart(PartialXMPPElement.from(xmlElement));
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmlElement.end();

        handleEnd(PartialXMPPElement.from(xmlElement));

        xmlElement = xmlElement.getParent().get(); // an element that wasn't
                                                   // open will never be closed,
                                                   // since the underlying
                                                   // stream is a valid XML one
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmlElement.appendToBody(reader);

        handleBody(PartialXMPPElement.from(xmlElement));
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }
}
