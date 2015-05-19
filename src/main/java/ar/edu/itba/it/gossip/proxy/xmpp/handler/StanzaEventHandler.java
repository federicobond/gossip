package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMPPStreamHandler handler;

    private PartialXMLElement xmlElement;

    public StanzaEventHandler(XMPPStreamHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handleStartDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleEndDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (xmlElement == null) {
            xmlElement = new PartialXMLElement();
        } else {
            PartialXMLElement newElement = new PartialXMLElement(xmlElement);
            xmlElement = newElement;
        }
        xmlElement.loadName(reader).loadAttributes(reader);

        if (xmlElement.getName().equals("stream:stream")) { // TODO:remove!
            handler.handleStart(PartialXMPPElement.from(xmlElement));
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmlElement.end();

        handler.handleEnd(PartialXMPPElement.from(xmlElement));

        xmlElement = xmlElement.getParent().get(); // an element that wasn't
                                                   // open will never be closed,
                                                   // since the underlying
                                                   // stream is a valid XML one
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmlElement.appendToBody(reader);
    }
}
