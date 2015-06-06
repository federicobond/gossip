package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.AUTHENTICATED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.AUTH_FAILED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.EXPECT_AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.INITIAL;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.LINKED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.VALIDATING_CREDENTIALS;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private State state = INITIAL;

    public OriginToClientXMPPStreamHandler(final XMPPConversation conversation,
            final OutputStream toClient, final OutputStream toOrigin)
            throws XMLStreamException {
        this.conversation = conversation;
        this.toOrigin = toOrigin;
        this.toClient = toClient;
    }

    @Override
    public void handleStart(PartialXMPPElement element) {
        switch (state) {
        case INITIAL:
            assumeType(element, STREAM_START);
            state = EXPECT_AUTH_FEATURES;
            break;
        case AUTHENTICATED:
            assumeType(element, STREAM_START);
            state = LINKED;
            System.out
                    .println("Origin is linked to the client, now messages may pass freely");

            sendDocumentStartToClient();
            sendToClient(element);
            break;
        case VALIDATING_CREDENTIALS:
            if (element.getType() == AUTH_FAILURE) {
                state = AUTH_FAILED;
                sendToClient(element);
            }
            break;
        case LINKED:
            sendToClient(element);
            break;
        default:
            switch (state) {
            case AUTH_FAILED:
            case LINKED:
                sendToClient(element);
                break;
            default:
                // do nothing TODO: change this!
            }
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        switch (state) {
        case AUTH_FAILED:
        case LINKED:
            sendToClient(element);
            break;
        default:
            break;
        }
    }

    @Override
    public void handleEnd(PartialXMPPElement element) {
        switch (state) { // FIXME: State pattern needed here!
        case EXPECT_AUTH_FEATURES:
            switch (element.getType()) {
            case AUTH_REGISTER:
            case AUTH_MECHANISMS:
            case AUTH_MECHANISM:
                break;
            case AUTH_FEATURES:
                sendAuthDataToOrigin();// FIXME: send the actual auth here!
                state = VALIDATING_CREDENTIALS;
                break;
            default:
                throw new IllegalStateException("Unexpected event type: "
                        + element.getType());
            }
            break;
        case VALIDATING_CREDENTIALS:
            switch (element.getType()) {
            case AUTH_SUCCESS:
                state = AUTHENTICATED;
                sendToClient(element);
                resetStream(); // TODO: check!
                break;
            case AUTH_FAILURE:// TODO
                sendToClient(element);
                break;
            default:
                throw new IllegalStateException("Unexpected event type: "
                        + element.getType());
            }
            break;
        case AUTH_FAILED:
            sendToClient(element);
            break;
        case LINKED:
            sendToClient(element);
            break;
        default:
            // will never happen
            throw new IllegalStateException("Unexpected state" + state);
        }
    }

    protected void sendAuthDataToOrigin() {
        writeTo(toOrigin,
                "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
                        + conversation.getCredentials().encode() + "</auth>");
    }

    private void sendDocumentStartToClient() {
        sendToClient("<?xml version=\"1.0\"?>");
    }

    protected void sendToClient(PartialXMPPElement element) {
        System.out.println("\n<O2C sending to client>");
        String currentContent = element.serializeCurrentContent();
        System.out.println("Message:\n'"
                + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
                + ArrayUtils.toString(currentContent.getBytes()));
        sendToClient(currentContent);
        System.out.println("\nOutgoing buffer afterwards:");
        ((ByteBufferOutputStream) toClient).printBuffer(false, true, true);
        System.out.println("</O2C sending to client>\n");
    }

    protected void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected enum State {
        INITIAL, EXPECT_AUTH_FEATURES, VALIDATING_CREDENTIALS, AUTHENTICATED, LINKED, AUTH_FAILED;
    }
}
