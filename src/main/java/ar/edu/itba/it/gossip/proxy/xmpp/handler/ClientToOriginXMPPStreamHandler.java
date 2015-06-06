package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.XMPPUtils.*;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.EXPECT_CREDENTIALS;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.INITIAL;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.LINKED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.MUTED_IN_MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.MUTED_OUTSIDE_MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.VALIDATING_CREDENTIALS;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.MutableChatState;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private static final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    private final XMPPConversation conversation;
    private final OutputStream toOrigin;
    private final OutputStream toClient;

    private State state = INITIAL;
    private boolean clientNotifiedOfMute;
    private boolean clientCauseOfMute;

    public ClientToOriginXMPPStreamHandler(final XMPPConversation conversation,
            final OutputStream toOrigin, final OutputStream toClient)
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
            sendStreamOpenToClient();
            sendToClient(streamFeatures("PLAIN"));
            state = EXPECT_CREDENTIALS;
            break;
        case VALIDATING_CREDENTIALS:
            // FIXME: do check that the credentials were actually valid! (the
            // code here is just assuming the client will behave and wait for an
            // auth <success>).
            assumeType(element, STREAM_START);
            state = LINKED;

            sendToOrigin(DOCUMENT_START);
            // fall through
        case LINKED:
            if (element.getType() == MESSAGE) {
                Message message = (Message) element;
                if (isMuted(message)) {
                    clientNotifiedOfMute = false;
                    clientCauseOfMute = isCurrentUserMuted();
                    state = MUTED_IN_MESSAGE;
                } else {
                    // TODO: check config to see if leet conversion should be
                    // enabled
                    message.enableLeetConversion();
                }
                // fall through
            }
            sendToOrigin(element);
            break;
        case MUTED_IN_MESSAGE:
            switch (element.getType()) {
            case BODY:
            case SUBJECT:
                if (!clientNotifiedOfMute) {
                    Message message = (Message) element.getParent().get();
                    sendMutedNotificationToClient(message);
                    clientNotifiedOfMute = true;
                }
                element.consumeCurrentContent();
                break;
            case COMPOSING:
            case PAUSED:
                ((MutableChatState) element).mute();
                // fall through
            default:
                sendToOrigin(element);
                break;
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
            if (element.getType() == MESSAGE) {
                clientNotifiedOfMute = false;
                state = MUTED_IN_MESSAGE;
            }
            sendToOrigin(element);
            break;
        default:
            // TODO: check! should NEVER happen!
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        switch (state) {
        case LINKED:
            // this is here just in case leet conversion was enabled by the
            // admin after the message's start tag
            if (element.getType() == MESSAGE) {
                ((Message) element).enableLeetConversion();
            }
            sendToOrigin(element);
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
                sendToOrigin(element);
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
            sendToOrigin(element);
            break;
        default:
            // do nothing, just buffer element's contents
            // TODO: check for potential floods!
        }
    }

    @Override
    public void handleEnd(PartialXMPPElement element) {
        switch (state) {
        case EXPECT_CREDENTIALS:
            assumeType(element, AUTH_CHOICE);
            Credentials credentials = ((Auth) element).getCredentials();
            conversation.setCredentials(credentials);
            System.out.println(credentials.getUsername()
                    + " is trying to log in with password: "
                    + credentials.getPassword());
            connectToOrigin();

            sendStreamOpenToOrigin();

            resetStream();

            state = VALIDATING_CREDENTIALS;
            break;
        case LINKED:
            sendToOrigin(element);
            break;
        case MUTED_IN_MESSAGE:
            switch (element.getType()) {
            case SUBJECT:
            case BODY:
                element.consumeCurrentContent();
                break;
            case MESSAGE:
                Message message = (Message) element;
                if (!isCurrentUserMuted()) {
                    // TODO: this assumes that messages cannot be embedded into
                    // other messages or anything like that! If that were the
                    // case, this *will* fail
                    state = LINKED;
                    if (clientCauseOfMute) {
                        sendUnmutedNotificationToClient(message);
                    }
                } else {
                    state = MUTED_OUTSIDE_MESSAGE;
                }
                // fall through
            default:
                sendToOrigin(element);
            }
            break;
        case MUTED_OUTSIDE_MESSAGE:
            sendToOrigin(element);
            break;
        default:
            throw new IllegalStateException("Unexpected state" + state);
        }
    }

    protected void connectToOrigin() {
        InetSocketAddress address = proxyConfig
                .getOriginAddress(getCurrentUser());
        getConnector().connectToOrigin(address);
    }

    private void sendStreamOpenToClient() {
        writeTo(toClient, DOCUMENT_START + streamOpen());
    }

    private void sendStreamOpenToOrigin() {
        String currentUser = getCurrentUser();
        writeTo(toOrigin,
                DOCUMENT_START
                        + streamOpen(currentUser, ProxyConfig.getInstance()
                                .getOriginName(currentUser)));
    }

    private void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected void sendToOrigin(PartialXMPPElement element) {
//        System.out.println("\n<C2O sending to origin>");
        String currentContent = element.serializeCurrentContent();
//        System.out.println("Message:\n'"
//                + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
//                + ArrayUtils.toString(currentContent.getBytes()));
        sendToOrigin(currentContent);
//        System.out.println("\nOutgoing buffer afterwards:");
//        ((ByteBufferOutputStream) toOrigin).printBuffer(false, true, true);
//        System.out.println("</C2O sending to origin>\n");
    }

    protected void sendToOrigin(String message) {
        writeTo(toOrigin, message);
    }

    private String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    protected boolean isMuted(Message message) {
        // TODO: change this!
        return isCurrentUserMuted() || message.getReceiver().contains("mute");
    }

    protected boolean isCurrentUserMuted() {
        // TODO: change this!
        return getCurrentUser().contains("mute");
    }

    private void sendMutedNotificationToClient(Message message) {
        final String msg;
        if (clientCauseOfMute) {
            msg = "You have been muted, you will not be able to talk to other users";
        } else {
            msg = message.getReceiver()
                    + " has been muted, you will not be able to talk to them";
        }
        sendNotificationToClient(message.getReceiver(), msg);
    }

    private void sendUnmutedNotificationToClient(Message message) {
        sendNotificationToClient(message.getReceiver(),
                "You are free to talk again");
    }

    private void sendNotificationToClient(String receiver, String text) {
        sendToClient(message(receiver, getCurrentUser(), text));
    }

    protected enum State {
        INITIAL, EXPECT_CREDENTIALS, VALIDATING_CREDENTIALS, LINKED, MUTED_OUTSIDE_MESSAGE, MUTED_IN_MESSAGE;
    }
}
