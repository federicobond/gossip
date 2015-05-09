package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.START_STREAM;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.AUTHENTICATED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.EXPECT_AUTH_STATUS;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthMechanism;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

public class OriginToClientXMPPStreamHandler extends XMLStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private Set<String> authMechanisms;

    private State state = State.INITIAL;

    public OriginToClientXMPPStreamHandler(final XMPPConversation conversation,
            final OutputStream toClient, final OutputStream toOrigin)
            throws XMLStreamException {
        this.conversation = conversation;
        this.toClient = toClient;
        this.toOrigin = toOrigin;

        authMechanisms = new HashSet<>();

        setEventHandler(new StanzaEventHandler(this));
    }

    @Override
    public void handle(XMPPEvent event) {
        switch (state) { // FIXME: State pattern needed here!
        case INITIAL:
            assumeEventType(event, START_STREAM);
            state = AUTH_FEATURES;
            break;
        case AUTH_FEATURES:
            switch (event.getType()) {
            case AUTH_REGISTER:
            case AUTH_MECHANISMS:
                break;
            case AUTH_MECHANISM:
                AuthMechanism authMech = (AuthMechanism) event;
                authMechanisms.add(authMech.getMechanism());
                break;
            case AUTH_FEATURES_END:
                // TODO: should probably fail gracefully if PLAIN isn't among
                // origin's
                // accepted auth mechanisms
                sendAuthDataToOrigin();
                state = EXPECT_AUTH_STATUS;
                break;
            default:
                throw new IllegalStateException("Unexpected event type: "
                        + event.getType());
            }
            break;
        case EXPECT_AUTH_STATUS:
            switch (event.getType()) {
            case AUTH_SUCCESS:
                state = AUTHENTICATED;
                sendAuthSuccessToClient();
                resetStream();
                break;
            // case AUTH_FAILURE://TODO
            default:
                throw new IllegalStateException("Unexpected event type: "
                        + event.getType());
            }
        case AUTHENTICATED:
            break;
        }
    }

    private void sendAuthDataToOrigin() {
        try {
            String auth = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
                    + conversation.getCredentials().encode() + "</auth>";
            toOrigin.write(auth.getBytes(UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAuthSuccessToClient() {
        try {
            toClient.write("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>"
                    .getBytes(UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected enum State {
        INITIAL, AUTH_FEATURES, EXPECT_AUTH_STATUS, AUTHENTICATED
    }

}
