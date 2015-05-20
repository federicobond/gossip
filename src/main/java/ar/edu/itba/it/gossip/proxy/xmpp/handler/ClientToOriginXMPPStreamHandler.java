package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.AuthState.AUTHENTICATED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.AuthState.INITIAL;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.AuthState.LINKED;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.AuthState.NEGOTIATING;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final ByteStream clientToOrigin;
    private final OutputStream toClient;

    private AuthState authState = INITIAL;

    public ClientToOriginXMPPStreamHandler(final XMPPConversation conversation,
            final ByteStream clientToOrigin, final OutputStream toClient)
            throws XMLStreamException {
        this.conversation = conversation;
        this.clientToOrigin = clientToOrigin;
        this.toClient = toClient;
    }

    @Override
    public void handleStart(PartialXMPPElement element) {
        switch (authState) {
        case INITIAL:
            assumeType(element, STREAM_START);
            sendStreamOpenToClient();
            sendStreamFeaturesToClient();
            authState = NEGOTIATING;
            break;
        case AUTHENTICATED:
            assumeType(element, STREAM_START);
            sendStreamOpenToOrigin();
            authState = LINKED;
            System.out
                    .println("Client is linked to origin, now messages may pass freely");
            break;
        case LINKED:
            sendToOrigin(element);
            break;
        default:
            // do nothing TODO: change this!
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        if (authState == LINKED) {
            sendToOrigin(element);
        }
    }

    @Override
    public void handleEnd(PartialXMPPElement element) {
        switch (authState) {
        case NEGOTIATING:
            assumeType(element, AUTH_CHOICE);
            Credentials credentials = ((Auth) element).getCredentials();
            conversation.setCredentials(credentials);
            System.out.println(credentials.getUsername()
                    + " is trying to log in with password: "
                    + credentials.getPassword());

            InetSocketAddress address = getOriginAddressForUsername(credentials
                    .getUsername());
            getConnector().connectToOrigin(address);

            sendStreamOpenToOrigin();
            resetStream();

            authState = AUTHENTICATED;
            break;
        case LINKED:
            sendToOrigin(element);
            break;
        default:
            // will never happen
            throw new IllegalStateException("Unexpected state" + authState);
        }
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

    private void sendToOrigin(PartialXMPPElement element) {
        String currentContent = element.serializeCurrentContent();
        System.out.println("C2O sending to origin: " + currentContent);
        sendToOrigin(currentContent);
    }

    private void sendToOrigin(String message) {
        writeTo(clientToOrigin, message);
    }

    private InetSocketAddress getOriginAddressForUsername(String username) {
        return new InetSocketAddress("localhost", 5222);
    }

    protected enum AuthState {
        INITIAL, NEGOTIATING, AUTHENTICATED, LINKED
    }
}
