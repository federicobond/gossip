package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

@RunWith(MockitoJUnitRunner.class)
public class ClientToOriginXMPPStreamHandlerTest extends
        AbstractXMPPStreamHandlerTest {
    private static final String DOCUMENT_START = "<?xml version=\"1.0\"?>";
    private static final String FAKE_STREAM_START_FOR_CLIENT = "<stream:stream "
            + "xmlns:stream='http://etherx.jabber.org/streams'"
            + " version='1.0' from='localhost'"
            + " id='6e5bb830-1e2d-40c3-8ebf-eacec740d83b'"
            + " xml:lang='en'"
            + " xmlns='jabber:toClient'>";
    private static final String FAKE_STREAM_AUTH_FEATURES = "<stream:features>\n"
            + "<register xmlns=\"http://jabber.org/features/iq-register\"/>\n"
            + "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">\n"
            + "<mechanism>PLAIN</mechanism>\n"
            + "</mechanisms>\n"
            + "</stream:features>";

    private static final String FAKE_STREAM_START_FOR_ORIGIN = "<stream:stream "
            + "xmlns:stream=\"http://etherx.jabber.org/streams\""
            + " version=\"1.0\""
            + " xmlns=\"jabber:client\""
            + " to=\"localhost\""
            + " xml:lang=\"en\""
            + " xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">";

    private static final String CURRENT_USER = "testUsername";
    private static final String MSG_RECEIVER = "testReceiver";

    private static final String MUTED_NOTIFICATION = "<message type=\"chat\" from=\""
            + MSG_RECEIVER
            + "\" to=\""
            + CURRENT_USER
            + "\">"
            + "<body>"
            + "You have been muted, you will not be able to talk to other users"
            + "</body>"
            + "<active xmlns=\"http://jabber.org/protocol/chatstates\"/>"
            + "</message>";

    private static final Credentials CREDENTIALS = new Credentials(
            CURRENT_USER, "testPassword");

    @Mock
    private XMPPConversation conversation;

    @Mock
    private ByteStream clientToOrigin;
    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestClientToOriginXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(CREDENTIALS);

        toOrigin = new ByteArrayOutputStream();
        when(clientToOrigin.getOutputStream()).thenReturn(toOrigin);

        toClient = new ByteArrayOutputStream();

        sut = new TestClientToOriginXMPPStreamHandler(conversation,
                clientToOrigin, toClient);
    }

    @Test
    public void testSendStreamStartAndFeaturesToClientOnConversationStart() {
        sut.handleStart(xmppElement(STREAM_START));
        assertEquals(DOCUMENT_START + FAKE_STREAM_START_FOR_CLIENT
                + FAKE_STREAM_AUTH_FEATURES, contents(toClient));
    }

    @Test
    public void testSetCredentialsAndStartConversationWithOriginOnAuthEnd() {
        startStream();
        toClient.reset();

        sendAuth();

        verify(conversation, times(1)).setCredentials(CREDENTIALS);
        assertEquals(1, sut.connectionAttempts);
        assertEquals(1, sut.streamResets);
        assertEquals(DOCUMENT_START + FAKE_STREAM_START_FOR_ORIGIN,
                contents(toOrigin));
    }

    @Test
    public void testSendDocumentStartAndOriginalStreamStartToOriginOnSecondStreamStart() {
        startStream();
        toClient.reset();

        sendAuth();
        toOrigin.reset();

        String clientStreamStartSerialization = "serialization of client's stream start";
        startStream(clientStreamStartSerialization);

        assertEquals(DOCUMENT_START + clientStreamStartSerialization,
                contents(toOrigin));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        startStream();
        toClient.reset();

        sendAuth();
        toOrigin.reset();

        startStream("serialization of client's stream start");
        toOrigin.reset();

        assertElementIsSentThroughToOrigin("<a>", "some text", "</a>");
        toOrigin.reset();
        assertElementIsSentThroughToOrigin("<b>", "some other text", "</b>");
    }

    @Test
    public void testClientIsNotifiedAndMessagesAreNotSentThroughOnMutedCurrentUser() {
        startStream();
        toClient.reset();

        sendAuth();
        toOrigin.reset();

        startStream("serialization of client's stream start");
        toOrigin.reset();

        sut.mutingUser = true;
        Message message = message(MSG_RECEIVER, "<message>", "message body",
                "</message>");

        sut.handleStart(message);
        assertEquals(MUTED_NOTIFICATION, contents(toClient));
        assertNothingWasSentThrough(toOrigin);
        toClient.reset();

        sut.handleBody(message);
        assertNothingWasSentThrough(toOrigin);
        assertNothingWasSentThrough(toClient);

        sut.handleEnd(message);
        assertNothingWasSentThrough(toOrigin);
        assertNothingWasSentThrough(toClient);
    }

    @Test
    public void testOnlyMessagesAreNotSentThroughOnMutedCurrentUser() {
        startStream();
        toClient.reset();

        sendAuth();
        toOrigin.reset();

        startStream("serialization of client's stream start");
        toOrigin.reset();

        sut.mutingUser = true;
        sendMessage("<message>", "message 1", "</message>");
        toClient.reset();

        assertElementIsSentThroughToOrigin("<a>", "some body", "</a>");
        toOrigin.reset();
        assertElementIsSentThroughToOrigin("<b>", "some body", "</b>");
        toOrigin.reset();

        sendMessage("<message>", "message 2", "</message>");
        assertNothingWasSentThrough(toOrigin);

        assertElementIsSentThroughToOrigin("<c>", "some body", "</c>");
    }

    private void sendMessage(String serialization0, String... serializations) {
        Message message = message(MSG_RECEIVER, serialization0, serializations);
        sut.handleStart(message);
        sut.handleBody(message);
        sut.handleEnd(message);
    }

    private void sendAuth() {
        Auth auth = auth(CREDENTIALS);
        sut.handleStart(auth);
        sut.handleEnd(auth);
    }

    private void assertElementIsSentThroughToOrigin(String startTag,
            String body, String endTag) {
        sendComplete(OTHER, startTag, body, endTag);
        assertEquals(startTag + body + endTag, contents(toOrigin));
    }

    private void assertNothingWasSentThrough(ByteArrayOutputStream stream) {
        assertTrue(contents(stream).isEmpty());
    }

    @Override
    protected XMPPStreamHandler getHandler() {
        return sut;
    }

    // class needed for method overrides
    private static class TestClientToOriginXMPPStreamHandler extends
            ClientToOriginXMPPStreamHandler {
        int connectionAttempts = 0;
        int streamResets = 0;
        boolean mutingUser = false;

        TestClientToOriginXMPPStreamHandler(XMPPConversation conversation,
                ByteStream clientToOrigin, OutputStream toClient)
                throws XMLStreamException {
            super(conversation, clientToOrigin, toClient);
        }

        @Override
        protected void connectToOrigin() { // stub
            connectionAttempts++;
        }

        @Override
        protected void resetStream() { // stub
            streamResets++;
        }

        @Override
        protected boolean isMutingCurrentUser() {
            return mutingUser;
        }

        @Override
        protected void sendToOrigin(PartialXMPPElement element) { // TODO:
                                                                  // remove this
                                                                  // once sysos
                                                                  // that do
                                                                  // casts are
                                                                  // removed
                                                                  // from the
                                                                  // original
                                                                  // method
            sendToOrigin(element.serializeCurrentContent());
        }
    }
}
