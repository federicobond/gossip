package ar.edu.itba.it.gossip.proxy.xmpp;

import java.util.HashMap;
import java.util.Map;

import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthStanza;
import ar.edu.itba.it.gossip.proxy.xmpp.event.ResponseStanza;
import ar.edu.itba.it.gossip.proxy.xmpp.event.StartStreamEvent;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMLStreamHandler handler;

    private State state = State.INITIAL;

    private String name;
    private String body;
    private Map<String, String> attributes;

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
        state = State.ELEMENT_STARTED;
        name = reader.getLocalName();

        if (name.equals("stream")) {
            handler.handle(new StartStreamEvent());
            name = null;
            state = State.INITIAL;
            return;
        }

        attributes = new HashMap<>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            attributes.put(reader.getAttributeLocalName(i),
                    reader.getAttributeValue(i));
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        state = State.INITIAL;

        final XMPPEvent event;
        switch (name) {
        case "stream":
            event = new StartStreamEvent();
            break;
        case "auth":
            event = new AuthStanza(attributes, body);
            break;
        case "response":
            event = new ResponseStanza(attributes, body);
            break;
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

    private enum State {
        INITIAL, ELEMENT_STARTED
    }
}
