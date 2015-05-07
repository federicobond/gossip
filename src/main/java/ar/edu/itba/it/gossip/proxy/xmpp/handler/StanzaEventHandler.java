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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;
import ar.edu.itba.it.gossip.util.Validations;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMLStreamHandler handler;
    private final Deque<String> names;

    private State state = State.INITIAL;

    private String body;
    private Map<String, String> attributes;

    public StanzaEventHandler(XMLStreamHandler handler) {
        this.handler = handler;
        this.names = new LinkedList<>();
    }

    @Override
    public void handleStartDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleEndDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        String name = reader.getLocalName();
        names.push(name);

        switch (name) {
        case "stream":
            handler.handle(XMPPEvent.from(START_STREAM, attributes, body));
            // TODO: shouldn't we let handleEndElement handle this?
            names.pop();
            state = State.INITIAL;
            break;
        case "features":
            state = AUTH_FEATURES;
            break;
        default:
            state = ELEMENT_STARTED;
            attributes = new HashMap<>();
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                attributes.put(reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
            }
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        String name = names.pop();

        final XMPPEvent event;
        switch (name) {
        case "stream":
            event = XMPPEvent.from(START_STREAM, attributes, body);
            this.state = INITIAL;
            break;
        case "auth":
            event = XMPPEvent.from(AUTH, attributes, body);
            this.state = INITIAL;
            break;
        case "response":
            event = XMPPEvent.from(RESPONSE, attributes, body);
            this.state = INITIAL;
            break;

        // TODO
        case "register":
            event = XMPPEvent.from(AUTH_REGISTER, attributes, body);
            break;
        case "mechanisms":
            event = XMPPEvent.from(AUTH_MECHANISMS, attributes, body);
            break;
        case "mechanism":
            event = XMPPEvent.from(AUTH_MECHANISM, attributes, body);
            break;
        case "features":
            event = XMPPEvent.from(AUTH_FEATURES_END, attributes, body);
            this.state = INITIAL;
            break;
        // TODO

        // these DON'T HAPPEN INSIDE AUTH_FEATURES
        case "success":
            event = XMPPEvent.from(AUTH_SUCCESS, attributes, body);
            break;
        case "failure":
            event = XMPPEvent.from(AUTH_FAILURE, attributes, body);
            break;
        // TODO

        default:
            throw new RuntimeException("unknown element: " + name);
        }

        handler.handle(event);
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        if (body == null) {
            body = reader.getText();
        } else {
            body = body + reader.getText();
        }
    }

    private void clearState() {
        if (state == INITIAL) {
            names.clear();
        }
        body = null;
        attributes = null; // TODO: check!
    }

    private void assumeState(State state) {
        Validations.assumeState(state == this.state,
                "State mismatch, got: %s when %s was expected", this.state,
                state);
    }

    protected enum State {
        INITIAL, ELEMENT_STARTED, AUTH_FEATURES
    }
}
