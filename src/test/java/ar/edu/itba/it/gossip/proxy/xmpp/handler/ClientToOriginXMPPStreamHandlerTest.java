package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandlerTestUtils.contents;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandlerTestUtils.xmppElement;
import static org.junit.Assert.assertEquals;
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
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

@RunWith(MockitoJUnitRunner.class)
public class ClientToOriginXMPPStreamHandlerTest {
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
    private static final Credentials credentials = new Credentials(
            "testUsername", "testPassword");

    @Mock
    private XMPPConversation conversation;

    @Mock
    private ByteStream clientToOrigin;
    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestClientToOriginXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(credentials);

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
        sut.handleStart(xmppElement(STREAM_START));
        toClient.reset();

        Auth auth = auth();
        sut.handleStart(auth);
        sut.handleEnd(auth);

        verify(conversation, times(1)).setCredentials(credentials);
        assertEquals(1, sut.connectionAttempts);
        assertEquals(1, sut.streamResets);
        assertEquals(DOCUMENT_START + FAKE_STREAM_START_FOR_ORIGIN,
                contents(toOrigin));
    }

    @Test
    public void testSendDocumentStartAndOriginalStreamStartToOriginOnSecondStreamStart() {
        sut.handleStart(xmppElement(STREAM_START));
        toClient.reset();

        Auth auth = auth();
        sut.handleStart(auth);
        sut.handleEnd(auth);
        toOrigin.reset();

        String clientStreamStartSerialization = "serialization of client's stream start";
        sut.handleStart(xmppElement(STREAM_START,
                clientStreamStartSerialization));

        assertEquals(DOCUMENT_START + clientStreamStartSerialization,
                contents(toOrigin));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        sut.handleStart(xmppElement(STREAM_START));
        toClient.reset();

        Auth auth = auth();
        sut.handleStart(auth);
        sut.handleEnd(auth);
        toOrigin.reset();

        String clientStreamStartSerialization = "serialization of client's stream start";
        sut.handleStart(xmppElement(STREAM_START,
                clientStreamStartSerialization));
        toOrigin.reset();

        assertMessageIsSentThroughToOrigin("<a>", "some text", "</a>");
        toOrigin.reset();
        assertMessageIsSentThroughToOrigin("<b>", "some other text", "</b>");
    }

    private void assertMessageIsSentThroughToOrigin(String startTag,
            String body, String endTag) {
        PartialXMPPElement element = xmppElement(OTHER);
        when(element.serializeCurrentContent()).thenReturn(startTag, body,
                endTag);

        sut.handleStart(element);
        sut.handleBody(element);
        sut.handleEnd(element);
        assertEquals(startTag + body + endTag, contents(toOrigin));
    }

    private Auth auth() {
        return XMPPStreamHandlerTestUtils.auth(credentials);
    }

    // class needed for method overrides
    private static class TestClientToOriginXMPPStreamHandler extends
            ClientToOriginXMPPStreamHandler {
        int connectionAttempts = 0;
        int streamResets = 0;

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
