package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.*;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthMechanism;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type;

public class OriginToClientXMPPStreamHandler extends XMLStreamHandler {
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private String username;
    private String password;

    private Set<String> authMechanisms;

    private State state = INITIAL;

    public OriginToClientXMPPStreamHandler(final OutputStream toClient,
            final OutputStream toOrigin) throws XMLStreamException {
        this.toClient = toClient;
        this.toOrigin = toOrigin;

        authMechanisms = new HashSet<>();

        setEventHandler(new StanzaEventHandler(this));
    }

    @Override
    public void handle(XMPPEvent event) {
        switch (state) {
        case INITIAL:
            assumeEventType(event, Type.START_STREAM);
            state = AUTH_FEATURES;
            break;
        case AUTH_FEATURES:
            switch (event.getType()) {
            case AUTH_MECHANISM:
                AuthMechanism authMech = (AuthMechanism) event;
                authMechanisms.add(authMech.getMechanism());
                break;
            case AUTH_FEATURES_END:
                break;
            default:
                throw new IllegalStateException("Unexpected event type: "
                        + event.getType());
            }
            break;
        default:
            break;
        }
    }

    protected enum State {
        INITIAL, AUTH_FEATURES,
    }

}
