package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.EXPECT_CREDENTIALS;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.INITIAL;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.State.LINKED;
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
            System.out
                    .println("Client is linked to origin, now messages may pass freely");

            sendDocumentStartToOrigin();
            // no break here, send things through
        case LINKED:
            sendToOrigin(element);
            break;
        default:
            // do nothing TODO: change this!
        }
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        if (state == LINKED) {
            sendToOrigin(element);
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

            InetSocketAddress address = getOriginAddressForUsername(credentials
                    .getUsername());
            getConnector().connectToOrigin(address);

            sendStreamOpenToOrigin();
            resetStream();

            state = VALIDATING_CREDENTIALS;
            break;
        case LINKED:
            sendToOrigin(element);
            break;
        default:
            // will never happen
            throw new IllegalStateException("Unexpected state" + state);
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

    private void sendToOrigin(String message) {
        writeTo(clientToOrigin, message);
    }

    private InetSocketAddress getOriginAddressForUsername(String username) {
        return new InetSocketAddress("localhost", 5222);
    }

    private void sendDocumentStartToOrigin() {
        sendToOrigin("<?xml version=\"1.0\"?>");
    }

    protected enum State {
        INITIAL, EXPECT_CREDENTIALS, VALIDATING_CREDENTIALS, LINKED
    }
}
