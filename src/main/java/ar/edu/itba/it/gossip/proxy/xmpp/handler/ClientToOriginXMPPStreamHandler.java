package ar.edu.itba.it.gossip.proxy.xmpp.handler;

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
import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.MutableChatState;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final ByteStream clientToOrigin;
    private final OutputStream toClient;


    private State state = INITIAL;
    private boolean clientNotifiedOfMute;

    private final ProxyConfig proxyConfig = ProxyConfig.getInstance();
    
    // Maybe this fits better in another class.
    // Somewhere we need to store the origin we are talking to, to be able to
    // restart the communication
    private String originName; 

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
            originName = proxyConfig.getOriginName();
            sendStreamOpenToClient(originName);
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
                if (isMutingCurrentUser()) {
                    clientNotifiedOfMute = false;
                    state = MUTED_IN_MESSAGE;
                } else {
                    if(proxyConfig.convertLeet()){
                        ((Message) element).enableLeetConversion();
                    }
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
            // do nothing, just buffer element's contents
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        switch (state) {
        case LINKED:
            // this is here just in case leet conversion was enabled by the
            // admin after the message's start tag
            if (element.getType() == MESSAGE && proxyConfig.convertLeet()) {
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

            //Update the origin name because might be changed by multiplexation. 
            originName = proxyConfig.getOriginName(credentials.getUsername());
			sendStreamOpenToOrigin(originName);
     
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
                if (!isMutingCurrentUser()) {
                    // TODO: this assumes that messages cannot be embedded into
                    // other messages or anything like that! If that were the
                    // case, this *will* fail
                    state = LINKED;
                    sendUnmutedNotificationToClient((Message) element);
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
        Credentials credentials = conversation.getCredentials();
        InetSocketAddress address = proxyConfig.getOriginAddress(credentials.getUsername());
        getConnector().connectToOrigin(address);
    }

    private void sendStreamOpenToOrigin(String originName) {
        // TODO: check "to" attribute. It fails if it does not match upstream host
        writeTo(clientToOrigin, "<?xml version=\"1.0\"?><stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"" + originName + "\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");

    }

    private void sendStreamOpenToClient(String originName) {
        writeTo(toClient, "<?xml version=\"1.0\"?><stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" from=\"" + originName + "\" id=\"6e5bb830-1e2d-40c3-8ebf-eacec740d83b\" xml:lang=\"en\" xmlns=\"jabber:toClient\">");
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
        sendNotificationToClient(message.getReceiver(),
                "You have been muted, you will not be able to talk to other users");
    }

    private void sendUnmutedNotificationToClient(Message message) {
        sendNotificationToClient(message.getReceiver(),
                "You are free to talk again");
    }

    private void sendNotificationToClient(String from, String text) {
        sendToClient("<message type=\"chat\" from=\"" + from + "\" to=\""
                + getCurrentUser() + "\">" + "<body>" + text + "</body>"
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

    private void sendDocumentStartToOrigin() {
        sendToOrigin("<?xml version=\"1.0\"?>");
    }

    protected enum State {
        INITIAL, EXPECT_CREDENTIALS, VALIDATING_CREDENTIALS, LINKED, MUTED_OUTSIDE_MESSAGE, MUTED_IN_MESSAGE
    }
}
