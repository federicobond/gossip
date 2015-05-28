package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandlerTestUtils.contents;
import static ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandlerTestUtils.xmppElement;
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
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

@RunWith(MockitoJUnitRunner.class)
public class OriginToClientXMPPStreamHandlerTest {
    private static final String DOCUMENT_START = "<?xml version=\"1.0\"?>";

    private static final Credentials credentials = new Credentials(
            "testUsername", "testPassword");

    private static final String FAKE_AUTH = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
            + credentials.encode() + "</auth>";

    @Mock
    private XMPPConversation conversation;

    @Mock
    private ByteStream originToClient;
    private ByteArrayOutputStream toOrigin;
    private ByteArrayOutputStream toClient;

    private TestOriginToClientXMPPStreamHandler sut;

    @Before
    public void setUp() throws XMLStreamException {
        when(conversation.getCredentials()).thenReturn(credentials);

        toClient = new ByteArrayOutputStream();
        when(originToClient.getOutputStream()).thenReturn(toClient);

        toOrigin = new ByteArrayOutputStream();

        sut = new TestOriginToClientXMPPStreamHandler(conversation,
                originToClient, toOrigin);
    }

    @Test
    public void testAuthSentOnConversationStart() {
        mockOriginsFirstConversationStart();

        assertEquals(FAKE_AUTH, contents(toOrigin));
    }

    @Test
    public void testSendFailureThroughOnAuthFailure() {
        mockOriginsFirstConversationStart();
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
        mockOriginsFirstConversationStart();
        toOrigin.reset();

        String successSerialization = "success serialization";
        PartialXMPPElement success = xmppElement(AUTH_SUCCESS,
                successSerialization);
        sut.handleStart(success);
        sut.handleEnd(success);
        assertEquals(successSerialization, contents(toClient));
        assertEquals(1, sut.streamResets);
    }

    @Test
    public void testSendStartDocumentAndOriginalStreamStartOnSecondStreamStart() {
        mockOriginsFirstConversationStart();
        toOrigin.reset();

        PartialXMPPElement success = xmppElement(AUTH_SUCCESS,
                "success serialization");
        sut.handleStart(success);
        sut.handleEnd(success);

        toClient.reset();

        String streamStartSerialization = "start serialization";
        sut.handleStart(xmppElement(STREAM_START, streamStartSerialization));
        assertEquals(DOCUMENT_START + streamStartSerialization,
                contents(toClient));
    }

    @Test
    public void testMessagesSentThroughAfterSecondStreamStart() {
        mockOriginsFirstConversationStart();
        toOrigin.reset();

        PartialXMPPElement success = xmppElement(AUTH_SUCCESS,
                "success serialization");
        sut.handleStart(success);
        sut.handleEnd(success);
        toClient.reset();

        sut.handleStart(xmppElement(STREAM_START, "start serialization"));
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<a>", "some body for a",
                "</a>");
        toClient.reset();

        assertElementIsSentThroughToClient(OTHER, "<b>", "some body for b",
                "</b>");
    }

    private void assertElementIsSentThroughToClient(Type type, String startTag,
            String... rest) {
        PartialXMPPElement element = xmppElement(type);
        when(element.serializeCurrentContent()).thenReturn(startTag, rest);

        sut.handleStart(element);
        if (rest.length > 0) {
            for (int i = 0; i < rest.length - 1; i++) {
                sut.handleBody(element);
            }
            if (rest.length > 0) {
                sut.handleEnd(element);
            }
        }

        String serialization = startTag + stream(rest).collect(joining());
        assertEquals(serialization, contents(toClient));
    }

    private Auth auth() {
        return XMPPStreamHandlerTestUtils.auth(credentials);
    }

    private void mockOriginsFirstConversationStart() {
        sut.handleStart(xmppElement(STREAM_START));

        sut.handleStart(xmppElement(AUTH_FEATURES));

        // register
        sut.handleStart(xmppElement(AUTH_REGISTER));
        sut.handleEnd(xmppElement(AUTH_REGISTER));

        // mechanisms
        sut.handleStart(xmppElement(AUTH_MECHANISMS));
        sut.handleStart(xmppElement(AUTH_MECHANISM));
        sut.handleEnd(xmppElement(AUTH_MECHANISM));
        sut.handleStart(xmppElement(AUTH_MECHANISM));
        sut.handleEnd(xmppElement(AUTH_MECHANISM));
        sut.handleEnd(xmppElement(AUTH_MECHANISMS));

        sut.handleEnd(xmppElement(AUTH_FEATURES));
    }

    // class needed for method overrides
    private static class TestOriginToClientXMPPStreamHandler extends
            OriginToClientXMPPStreamHandler {

        int streamResets = 0;

        TestOriginToClientXMPPStreamHandler(XMPPConversation conversation,
                ByteStream originToClient, OutputStream toOrigin)
                throws XMLStreamException {
            super(conversation, originToClient, toOrigin);
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
