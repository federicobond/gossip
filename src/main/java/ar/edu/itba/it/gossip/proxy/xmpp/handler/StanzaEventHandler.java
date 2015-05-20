package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPEventHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMPPEventHandler xmppHandler;

    private PartialXMLElement xmlElement;

    public StanzaEventHandler(final XMPPEventHandler handler) {
        this.xmppHandler = handler;
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (xmlElement == null) {
            xmlElement = new PartialXMLElement();
        } else {
            xmlElement = new PartialXMLElement(xmlElement);
        }
        xmlElement.loadName(reader).loadAttributes(reader);

        xmppHandler.handleStart(PartialXMPPElement.from(xmlElement));
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmlElement.end();

        xmppHandler.handleEnd(PartialXMPPElement.from(xmlElement));

        xmlElement = xmlElement.getParent().get(); // an element that wasn't
                                                   // open will never be closed,
                                                   // since the underlying
                                                   // stream is a valid XML one
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmlElement.appendToBody(reader);

        xmppHandler.handleBody(PartialXMPPElement.from(xmlElement));
    }
}
