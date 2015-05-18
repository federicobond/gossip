package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.event.AuthStanza;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import javax.xml.stream.XMLStreamException;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.*;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.ClientToOriginXMPPStreamHandler.AuthState.*;

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

        setEventHandler(new StanzaEventHandler(this));
    }

    @Override
    public void handle(XMPPEvent event) {
        switch (authState) {
        case INITIAL:
            assumeEventType(event, STREAM_START);
            sendStreamOpenToClient();
            sendStreamFeaturesToClient();
            authState = NEGOTIATING;
            break;
        case NEGOTIATING:
            assumeEventType(event, AUTH_CHOICE);
            AuthStanza auth = (AuthStanza) event;
            Credentials credentials = auth.getCredentials();
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
        case AUTHENTICATED:
            assumeEventType(event, STREAM_START);
            sendStreamOpenToOrigin();

            authState = LINKED;
            System.out
                    .println("Client is linked to origin, now messages may pass freely");
            break;
        case LINKED:

            PartialXMLElement element = event.getElement();
            String currentContent = element.serializeCurrentContent();
            System.out.println(currentContent);
            sendToOrigin(currentContent);
            // clientToOrigin.flush();
            break;
        }
    }

    private void sendStreamOpenToOrigin() {
        // TODO: check "to" attribute. It fails if it does not match upstream
        // host
        sendToOrigin("<?xml version=\"1.0\"?><stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\" xmlns=\"jabber:client\" to=\"localhost\" xml:lang=\"en\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
    }

    private void sendStreamOpenToClient() {
        writeTo(toClient,
                "<?xml version=\"1.0\"?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='localhost' id='6e5bb830-1e2d-40c3-8ebf-eacec740d83b' xml:lang='en' xmlns='jabber:toClient'>");
    }

    private void sendStreamFeaturesToClient() {
        writeTo(toClient,
                "<stream:features>\n"
                        + "<register xmlns=\"http://jabber.org/features/iq-register\"/>\n"
                        + "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">\n"
                        + "<mechanism>PLAIN</mechanism>\n" + "</mechanisms>\n"
                        + "</stream:features>");
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
