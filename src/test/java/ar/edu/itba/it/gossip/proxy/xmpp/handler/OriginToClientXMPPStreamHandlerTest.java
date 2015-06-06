package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
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
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

@RunWith(MockitoJUnitRunner.class)
public class OriginToClientXMPPStreamHandlerTest extends
        AbstractXMPPStreamHandlerTest {
    private static final String DOCUMENT_START = "<?xml version=\"1.0\"?>";

    private static final Credentials credentials = new Credentials(
            "testUsername", "testPassword");

    private static final String FAKE_AUTH = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
            + credentials.encode() + "</auth>";

    @Mock
    private XMPPConversation conversation;

    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestOriginToClientXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(credentials);

        toClient = new ByteArrayOutputStream();
        toOrigin = new ByteArrayOutputStream();

        sut = new TestOriginToClientXMPPStreamHandler(conversation, toClient,
                toOrigin);
    }

    @Test
    public void testAuthSentOnConversationStart() {
        sendOriginsFirstStart();

        assertEquals(FAKE_AUTH, contents(toOrigin));
    }

    @Test
    public void testSendFailureThroughOnAuthFailure() {
        sendOriginsFirstStart();
        toOrigin.reset();

        String[] serializations = { "failure serialization start",
                "not authorized serialization", "text serialization start",
                "text serialization", "text serialization end",
                "failure serialization end" };
        PartialXMPPElement failure = xmppElement(AUTH_FAILURE,
                serializations[0], serializations[5]);
        PartialXMPPElement notAuthorized = xmppElement(OTHER, serializations[1]);
        PartialXMPPElement text = xmppElement(OTHER, serializations[2],
                serializations[3], serializations[4]);

        sut.handleStart(failure);
        assertEquals(serializations[0], contents(toClient));
        toClient.reset();

        sut.handleEnd(notAuthorized);
        assertEquals(serializations[1], contents(toClient));
        toClient.reset();

        sut.handleStart(text);
        assertEquals(serializations[2], contents(toClient));
        toClient.reset();

        sut.handleBody(text);
        assertEquals(serializations[3], contents(toClient));
        toClient.reset();

        sut.handleEnd(text);
        assertEquals(serializations[4], contents(toClient));
        toClient.reset();

        sut.handleEnd(failure);
        assertEquals(serializations[5], contents(toClient));
        toClient.reset();
    }

    @Test
    public void testSendSuccessAndResetStreamThroughOnAuthSuccess() {
        sendOriginsFirstStart();
        toOrigin.reset();

        String successSerialization = "success serialization";
        sendAuthSuccess(successSerialization);

        assertEquals(successSerialization, contents(toClient));
        assertEquals(1, sut.streamResets);
    }

    @Test
    public void testSendStartDocumentAndOriginalStreamStartOnSecondStreamStart() {
        sendOriginsFirstStart();
        toOrigin.reset();

        sendAuthSuccess();
        toClient.reset();

        String streamStartSerialization = "start serialization";
        startStream(streamStartSerialization);
        assertEquals(DOCUMENT_START + streamStartSerialization,
                contents(toClient));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        sendOriginsFirstStart();
        toOrigin.reset();

        sendAuthSuccess();
        toClient.reset();

        startStream();
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<a>", "some body for a",
                "</a>");
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<b>", "some body for b",
                "</b>");
    }

    private void sendOriginsFirstStart() {
        sut.handleStart(xmppElement(STREAM_START));
        // features
        sut.handleStart(xmppElement(AUTH_FEATURES));
        // register
        sendComplete(AUTH_REGISTER, "", "");
        // mechanisms
        sut.handleStart(xmppElement(AUTH_MECHANISMS));
        sendComplete(AUTH_MECHANISM, "", "");
        sendComplete(AUTH_MECHANISM, "", "");
        sut.handleEnd(xmppElement(AUTH_MECHANISMS));
        sut.handleEnd(xmppElement(AUTH_FEATURES));
    }

    private void sendAuthSuccess(String serialization) {
        sendComplete(AUTH_SUCCESS, serialization);
    }

    private void sendAuthSuccess() {
        sendAuthSuccess("");
    }

    private void assertElementIsSentThroughToClient(Type type, String startTag,
            String... rest) {
        sendComplete(type, startTag, rest);

        String serialization = startTag + stream(rest).collect(joining());
        assertEquals(serialization, contents(toClient));
    }

    @Override
    protected XMPPStreamHandler getSUT() {
        return sut;
    }

    // class needed for method overrides
    private static class TestOriginToClientXMPPStreamHandler extends
            OriginToClientXMPPStreamHandler {

        int streamResets = 0;

        TestOriginToClientXMPPStreamHandler(XMPPConversation conversation,
                OutputStream toClient, OutputStream toOrigin)
                throws XMLStreamException {
            super(conversation, toClient, toOrigin);
        }

        @Override
        protected void resetStream() { // stub
            streamResets++;
        }

        @Override
        protected void sendToClient(PartialXMPPElement element) { // TODO:
                                                                  // remove this
                                                                  // once sysos
                                                                  // that do
                                                                  // casts are
                                                                  // removed
                                                                  // from the
                                                                  // original
                                                                  // method
            sendToClient(element.serializeCurrentContent());
        }
    }
}
