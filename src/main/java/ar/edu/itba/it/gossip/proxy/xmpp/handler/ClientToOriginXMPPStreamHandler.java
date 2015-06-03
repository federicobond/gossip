package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.EXPECT_CREDENTIALS;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.INITIAL;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.IN_MUTED_MESSAGE;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.LINKED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.MUTED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.VALIDATING_CREDENTIALS;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final ByteStream clientToOrigin;
    private final OutputStream toClient;

    private State state = INITIAL;

    public ClientToOriginXMPPStreamHandler(final XMPPConversation conversation,
            final ByteStream clientToOrigin, final OutputStream toClient)
            throws XMLStreamException {
        this.conversation = conversation;
        this.clientToOrigin = clientToOrigin;
        this.toClient = toClient;
    }

    @Override
    public void handleStart(PartialXMPPElement element) {
        switch (state) {
        case INITIAL:
            assumeType(element, STREAM_START);
            sendStreamOpenToClient();
            sendStreamFeaturesToClient();
            state = EXPECT_CREDENTIALS;
            break;
        case VALIDATING_CREDENTIALS:
            // FIXME: do check that the credentials were actually valid! (the
            // code here is just assuming the client will behave and wait for an
            // auth <success>).
            assumeType(element, STREAM_START);
            state = LINKED;
            System.out.println("Client is linked to origin,"
                    + " now messages may pass freely");

            sendDocumentStartToOrigin();
            // fall through
        case LINKED:
            if (element.getType() == MESSAGE) {
                Message message = (Message) element;
                if (isMutingCurrentUser()) {
                    sendMutedNotificationToClient(message);
                    state = IN_MUTED_MESSAGE;
                    return;
                }
                message.enableLeetConversion();
                // fall through
            }
            sendToOrigin(element);
            break;
        case IN_MUTED_MESSAGE:
            element.consumeCurrentContent();
            break;
        case MUTED:
            if (element.getType() == MESSAGE) {
                sendMutedNotificationToClient((Message) element); // TODO:
                                                                  // probably
                                                                  // should
                                                                  // remove
                                                                  // this, since
                                                                  // it is also
                                                                  // sending
                                                                  // messages to
                                                                  // the user
                                                                  // when they
                                                                  // start or
                                                                  // stop
                                                                  // typingmonitor
                state = IN_MUTED_MESSAGE;
                return;
            }
            sendToOrigin(element);
            break;
        default:
            // do nothing, just buffer element's contents
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
        case IN_MUTED_MESSAGE:
            element.consumeCurrentContent();
            break;
        case MUTED:
            sendToOrigin(element);
            break;
        default:
            // do nothing, just buffer element's contents
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
        case IN_MUTED_MESSAGE:
            element.consumeCurrentContent();
            if (element.getType() == MESSAGE) {
                if (!isMutingCurrentUser()) {
                    // TODO: this assumes that messages cannot be embedded into
                    // other messages or anything like that! If that were the
                    // case,
                    // this *will fail*
                    state = LINKED;
                    // TODO: send the poor guy a message that he can start
                    // talking again!
                } else {
                    state = MUTED;
                }
            }
            break;
        case MUTED:
            sendToOrigin(element);
            break;
        default:
            throw new IllegalStateException("Unexpected state" + state);
        }
    }

    protected void connectToOrigin() {
        Credentials credentials = conversation.getCredentials();
        InetSocketAddress address = getOriginAddressForUsername(credentials
                .getUsername());
        getConnector().connectToOrigin(address);
    }

    private void sendStreamOpenToOrigin() {
        // TODO: check "to" attribute. It fails if it does not match upstream
        // host
        sendToOrigin("<?xml version=\"1.0\"?><stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
    }

    private void sendStreamOpenToClient() {
        sendToClient("<?xml version=\"1.0\"?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='localhost' id='6e5bb830-1e2d-40c3-8ebf-eacec740d83b' xml:lang='en' xmlns='jabber:toClient'>");
    }

    private void sendStreamFeaturesToClient() {
        sendToClient("<stream:features>\n"
                + "<register xmlns=\"http://jabber.org/features/iq-register\"/>\n"
                + "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">\n"
                + "<mechanism>PLAIN</mechanism>\n" + "</mechanisms>\n"
                + "</stream:features>");
    }

    private void sendToClient(String message) {
        writeTo(toClient, message);
    }

    private void sendMutedNotificationToClient(Message message) {
        // TODO: check if this shouldn't be a custom error!
        sendToClient("<message type=\"chat\" from=\""
                + message.getReceiver()
                + "\" to=\""
                + getCurrentUser()
                + "\">"
                + "<body>"
                + "You have been muted, you will not be able to talk to other users"
                + "</body>"
                + "<active xmlns=\"http://jabber.org/protocol/chatstates\"/>"
                + "</message>");
    }

    protected void sendToOrigin(PartialXMPPElement element) {
        System.out.println("\n<C2O sending to origin>");
        String currentContent = element.serializeCurrentContent();
        System.out.println("Message:\n'"
                + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
                + ArrayUtils.toString(currentContent.getBytes()));
        sendToOrigin(currentContent);
        System.out.println("\nOutgoing buffer afterwards:");
        ((ByteBufferOutputStream) clientToOrigin.getOutputStream())
                .printBuffer(false, true, true);
        System.out.println("</C2O sending to origin>\n");
    }

    protected void sendToOrigin(String message) {
        writeTo(clientToOrigin, message);
    }

    private String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    protected boolean isMutingCurrentUser() {
        // TODO: change this!
        return getCurrentUser().contains("mute");
    }

    private InetSocketAddress getOriginAddressForUsername(String username) {
        return new InetSocketAddress("localhost", 5222);
    }

    private void sendDocumentStartToOrigin() {
        sendToOrigin("<?xml version=\"1.0\"?>");
    }

    protected enum State {
        INITIAL, EXPECT_CREDENTIALS, VALIDATING_CREDENTIALS, LINKED, MUTED, IN_MUTED_MESSAGE
    }
}
