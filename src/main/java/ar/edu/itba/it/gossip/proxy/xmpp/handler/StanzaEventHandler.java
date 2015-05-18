package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.STREAM_START;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMPPStreamHandler handler;

    private PartialXMLElement element;

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
        if (element == null) {
            element = new PartialXMLElement();
        } else {
            PartialXMLElement newElement = new PartialXMLElement(element);
            this.element = newElement;
        }
        element.loadName(reader).loadAttributes(reader);

        if (element.getName().equals("stream")) {
            XMPPEvent event = XMPPEvent.from(STREAM_START, element);
            handler.handle(event);
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        element.end();

        XMPPEvent event = XMPPEvent.from(getEventType(element.getName()),
                element);
        handler.handle(event);

        element = element.getParent().get(); // an element that wasn't open
                                             // will never be closed, since
                                             // the underlying stream is a
                                             // valid XML one
    }

    private XMPPEvent.Type getEventType(String name) {
        switch (name) {
        // stanzas from client
        case "auth":
            return AUTH_CHOICE;
        // stanzas from client

        // stanzas from origin
        case "features":
            return AUTH_FEATURES;
        case "register":
            return AUTH_REGISTER;
        case "mechanisms":
            return AUTH_MECHANISMS;
        case "mechanism":
            return AUTH_MECHANISM;

        case "success":
            return AUTH_SUCCESS;
        case "failure":
            return AUTH_FAILURE;
        // stanzas from origin

        // stanzas from both
        case "stream":
            // NOTE: this is here just as a reminder that it should go here once
            // we deal with XMPP events at their start (and body, and end)

            // TODO: verify which stream open this is!
            // (also whether it is the first or second!)
            return STREAM_START;
        default:
            return OTHER;
        // stanzas from both
        }
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        element.appendToBody(reader);
    }
}
