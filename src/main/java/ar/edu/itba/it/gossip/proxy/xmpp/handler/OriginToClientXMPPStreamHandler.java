package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.*;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.*;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthMechanism;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final ByteStream originToClient;
    private final OutputStream toOrigin;

    private Set<String> authMechanisms;

    private State state = INITIAL;

    public OriginToClientXMPPStreamHandler(final XMPPConversation conversation,
            final ByteStream originToClient, final OutputStream toOrigin)
            throws XMLStreamException {
        this.conversation = conversation;
        this.toOrigin = toOrigin;
        this.originToClient = originToClient;

        authMechanisms = new HashSet<>();

        setEventHandler(new StanzaEventHandler(this));
    }

    @Override
    public void handle(XMPPEvent event) {
        switch (state) { // FIXME: State pattern needed here!
        case INITIAL:
            assumeEventType(event, STREAM_START);
            state = EXPECT_AUTH_FEATURES;
            break;
        case EXPECT_AUTH_FEATURES:
            switch (event.getType()) {
            case AUTH_REGISTER:
            case AUTH_MECHANISMS:
                break;
            case AUTH_MECHANISM:
                AuthMechanism authMech = (AuthMechanism) event;
                authMechanisms.add(authMech.getMechanism());
                break;
            case AUTH_FEATURES:
                // TODO: should probably fail gracefully if PLAIN isn't among
                // origin's accepted auth mechanisms
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
            break;
        case AUTHENTICATED:
            assumeEventType(event, STREAM_START);
            state = LINKED;
            System.out
                    .println("Origin is linked to the client, now messages may pass freely");
            // NOTE: no break needed here *YET*, as of now they both end up
            // flushing
        case LINKED:
            PartialXMLElement element = event.getElement();
            String currentContent = element.serializeCurrentContent();
            System.out.println(currentContent);
            sendToClient(currentContent);
            // originToClient.flush();
        }
    }

    private void sendAuthDataToOrigin() {
        writeTo(toOrigin,
                "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
                        + conversation.getCredentials().encode() + "</auth>");
    }

    private void sendAuthSuccessToClient() {
        sendToClient("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>");
    }

    private void sendToClient(String message) {
        writeTo(originToClient, message);
    }

    protected enum State {
        INITIAL, EXPECT_AUTH_FEATURES, EXPECT_AUTH_STATUS, AUTHENTICATED, LINKED
    }
}
