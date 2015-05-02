package ar.edu.itba.it.gossip.xmpp;

import ar.edu.itba.it.gossip.xml.EventHandler;
import ar.edu.itba.it.gossip.xmpp.event.AuthStanza;
import ar.edu.itba.it.gossip.xmpp.event.Event;
import ar.edu.itba.it.gossip.xmpp.event.ResponseStanza;
import ar.edu.itba.it.gossip.xmpp.event.StartStreamEvent;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.HashMap;
import java.util.Map;

public class StanzaEventHandler implements EventHandler {

    private final StreamHandler handler;

    private State state = State.INITIAL;

    private String name;
    private String body;
    private Map<String, String> attributes;

    public StanzaEventHandler(StreamHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onStartDocument(AsyncXMLStreamReader<?> reader) {

    }

    @Override
    public void onEndDocument(AsyncXMLStreamReader<?> reader) {

    }

    @Override
    public void onStartElement(AsyncXMLStreamReader<?> reader) {
        state = State.ELEMENT_STARTED;
        name = reader.getLocalName();

        if (name.equals("stream")) {
            handler.processEvent(new StartStreamEvent());
            name = null;
            state = State.INITIAL;
            return;
        }

        attributes = new HashMap<>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
    }

    @Override
    public void onEndElement(AsyncXMLStreamReader<?> reader) {
        state = State.INITIAL;

        final Event event;
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
        handler.processEvent(event);
    }

    @Override
    public void onCharacters(AsyncXMLStreamReader<?> reader) {
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
