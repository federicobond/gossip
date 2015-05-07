package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.ELEMENTS;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.ELEMENT_STARTED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.StanzaEventHandler.State.INITIAL;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthMechanism;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthFeaturesEnd;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthStanza;
import ar.edu.itba.it.gossip.proxy.xmpp.event.ResponseStanza;
import ar.edu.itba.it.gossip.proxy.xmpp.event.StartStreamEvent;
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
            handler.handle(new StartStreamEvent()); // TODO: shouldn't we let
                                                    // handleEndElement handle
                                                    // this?
            names.pop();
            state = State.INITIAL;
            break;
        case "features":
            state = ELEMENTS;
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
            event = new StartStreamEvent();
            state = INITIAL;
            break;
        case "auth":
            event = new AuthStanza(attributes, body);
            state = INITIAL;
            break;
        case "response":
            event = new ResponseStanza(attributes, body);
            state = INITIAL;
            break;

        // TODO
        case "register":
        case "mechanisms":
            event = null; // to denote nothing happened
            break;
        case "mechanism":
            event = new AuthMechanism(attributes, body);
            break;
        case "features":
            event = new AuthFeaturesEnd();
            state = INITIAL;
            break;
        // TODO

        default:
            throw new RuntimeException("unknown element: " + name);
        }

        if (state == INITIAL) {
            names.clear();
        }
        body = null;

        if (event != null) {
            handler.handle(event);
        }
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        if (body == null) {
            body = reader.getText();
        } else {
            body = body + reader.getText();
        }
    }

    private void assumeState(State state) {
        Validations.assumeState(state == this.state,
                "State mismatch, got: %s when %s was expected", this.state,
                state);
    }

    protected enum State {
        INITIAL, ELEMENT_STARTED, ELEMENTS
    }
}
