package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FEATURES_END;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.RESPONSE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.START_STREAM;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.ELEMENT_STARTED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.INITIAL;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import ar.edu.itba.it.gossip.proxy.xml.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMLStreamHandler handler;

    private int i = 0;
    private State state = State.INITIAL;

    private PartialXMLElement element;

    public StanzaEventHandler(XMLStreamHandler handler) {
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
        final PartialXMLElement newElement;
        if (element == null) {
            newElement = new PartialXMLElement();
        } else {
            newElement = new PartialXMLElement(element);
            this.element = newElement;
        }

        newElement.loadName(reader);
        String name = newElement.getName();

        //aca habria que llamar al handler padre para que se avive de que esta pasando
        switch (name) {
        case "stream":
            handler.handle(XMPPEvent.from(START_STREAM, newElement));
            state = INITIAL;
            break;
        case "features":
            state = AUTH_FEATURES;
            break;
        default:
            state = ELEMENT_STARTED;
            newElement.loadAttributes(reader);
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        final XMPPEvent event;
        switch (element.getName()) {
        case "auth":
            event = XMPPEvent.from(AUTH, element);
            this.state = INITIAL;
            break;
        case "response":
            event = XMPPEvent.from(RESPONSE, element);
            this.state = INITIAL;
            break;

        // TODO
        case "register":
            event = XMPPEvent.from(AUTH_REGISTER, element);
            break;
        case "mechanisms":
            event = XMPPEvent.from(AUTH_MECHANISMS, element);
            break;
        case "mechanism":
            event = XMPPEvent.from(AUTH_MECHANISM, element);
            break;
        case "features":
            event = XMPPEvent.from(AUTH_FEATURES_END, element);
            this.state = INITIAL;
            break;
        // TODO

        // these DON'T HAPPEN INSIDE AUTH_FEATURES
        case "success":
            event = XMPPEvent.from(AUTH_SUCCESS, element);
            break;
        case "failure":
            event = XMPPEvent.from(AUTH_FAILURE, element);
            break;
        // TODO

        default:
            throw new RuntimeException("unknown element: " + element.getName());
        }

        element = null;
        handler.handle(event);
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        // llamar al handler padre para que mande la orden de appendear
        element.appendToBody(reader);
    }

    protected enum State {
        INITIAL, ELEMENT_STARTED, AUTH_FEATURES
    }
}
