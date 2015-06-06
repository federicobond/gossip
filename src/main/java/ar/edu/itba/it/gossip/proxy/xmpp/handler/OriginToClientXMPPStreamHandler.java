package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.OriginToClientXMPPStreamHandler.State.*;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.MutableChatState;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private State state = INITIAL;

    // private boolean otherNotifiedOfMute;

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
        case AUTH_FAILED:
            sendToClient(element);
            // TODO: should terminate the connection or something here!
            break;
        case LINKED:
            if (element.getType() == MESSAGE) {
                if (isMutingSender((Message) element)) {
                    // otherNotifiedOfMute = false;
                    state = MUTED_IN_MESSAGE;
                } else {
                    // TODO: if you want to convert messages from outside origin
                    // to leet, this is the place!
                }
                // fall through
            }
            sendToClient(element);
            break;
        case MUTED_IN_MESSAGE:
            switch (element.getType()) {
            case BODY:
            case SUBJECT:
                // if(!otherNotifiedOfMute) {
                // Message message = (Message) element.getParent().get();
                // sendMutedNotificationToSender(message);
                // otherNotifiedOfMute = true;
                // }
                element.consumeCurrentContent();
                break;
            case COMPOSING:
            case PAUSED:
                ((MutableChatState) element).mute();
                // fall through
            default:
                sendToClient(element);
                break;
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
            if (element.getType() == MESSAGE) {
                // otherNotifiedOfMute = false;
                state = MUTED_IN_MESSAGE;
            }
            sendToClient(element);
            break;
        default:
            // TODO: check! should NEVER happen!
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        switch (state) {
        case AUTH_FAILED:
            sendToClient(element);
            break;
        case LINKED:
            // TODO: check for leet case!
            sendToClient(element);
            break;
        case MUTED_IN_MESSAGE:
            switch (element.getType()) {
            case SUBJECT:
            case BODY:
            case MESSAGE:// you are muted, don't try and send text in message
                         // tags =)
                element.consumeCurrentContent();
                break;
            default:
                sendToClient(element);
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
            sendToClient(element);
            break;
        default:
            // do nothing, just buffer element's contents
            // TODO: check for potential floods!
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
                sendAuthDataToOrigin(); // FIXME: send the actual auth here!
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
                resetStream();
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
        case MUTED_IN_MESSAGE:
            switch (element.getType()) {
            case SUBJECT:
            case BODY:
                element.consumeCurrentContent();
                break;
            case MESSAGE:
                if (!isMutingSender((Message) element)) {
                    // TODO: this assumes that messages cannot be embedded into
                    // other messages or anything like that! If that were the
                    // case, this *will* fail
                    state = LINKED;
                    // sendUnmutedNotificationToSender((Message) element);
                } else {
                    state = MUTED_OUTSIDE_MESSAGE;
                }
                // fall through
            default:
                sendToClient(element);
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
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

    protected boolean isMutingSender(Message message) {
        // TODO: change this!
        return message.getSender().contains("mute");
    }

    private String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    protected enum State {
        INITIAL, EXPECT_AUTH_FEATURES, VALIDATING_CREDENTIALS, AUTHENTICATED, LINKED, AUTH_FAILED, MUTED_IN_MESSAGE, MUTED_OUTSIDE_MESSAGE;
    }
}
