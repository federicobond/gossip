package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.*;
import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

abstract class AbstractXMPPStreamHandlerTest {
    protected abstract XMPPStreamHandler getHandler();

    protected void startStream() {
        startStream("");
    }

    protected void startStream(String serialization) {
        getHandler().handleStart(xmppElement(STREAM_START, serialization));
    }

    protected String contents(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), UTF_8);
    }

    protected Auth auth(Credentials credentials) {
        Auth mockAuth = mock(Auth.class);
        when(mockAuth.getType()).thenReturn(AUTH_CHOICE);
        when(mockAuth.getCredentials()).thenReturn(credentials);
        return mockAuth;
    }

    protected Message message(String receiver, String serialization0,
            String... serializations) {
        Message mockMessage = mock(Message.class);
        when(mockMessage.getType()).thenReturn(MESSAGE);
        when(mockMessage.getReceiver()).thenReturn(receiver);
        when(mockMessage.serializeCurrentContent()).thenReturn(serialization0,
                serializations);
        return mockMessage;
    }

    protected PartialXMPPElement xmppElement(Type type) {
        PartialXMPPElement mockElement = mock(PartialXMPPElement.class);
        when(mockElement.getType()).thenReturn(type);
        return mockElement;
    }

    protected PartialXMPPElement xmppElement(Type type, String serialization0,
            String... serializations) {
        PartialXMPPElement mockElement = xmppElement(type);
        when(mockElement.serializeCurrentContent()).thenReturn(serialization0,
                serializations);
        return mockElement;
    }

    protected void sendComplete(Type type, String serialization0,
            String... serializations) {
        send(type, true, serialization0, serializations);
    }

    protected void sendUnfinished(Type type, String serialization0,
            String... serializations) {
        send(type, false, serialization0, serializations);
    }

    private void send(Type type, boolean ends, String serialization0,
            String... serializations) {
        PartialXMPPElement element = xmppElement(type, serialization0,
                serializations);

        getHandler().handleStart(element);

        int end = ends ? serializations.length - 1 : serializations.length;
        for (int i = 0; i < end; i++) {
            getHandler().handleBody(element);
        }

        if (ends) {
            getHandler().handleEnd(element);
        }
    }
}
