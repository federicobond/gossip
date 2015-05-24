package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

abstract class XMPPStreamHandlerTestUtils {
    static String contents(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), UTF_8);
    }

    static Auth auth(Credentials credentials) {
        Auth mockAuth = mock(Auth.class);
        when(mockAuth.getType()).thenReturn(AUTH_CHOICE);
        when(mockAuth.getCredentials()).thenReturn(credentials);
        return mockAuth;
    }

    static PartialXMPPElement xmppElement(Type type) {
        PartialXMPPElement mockElement = mock(PartialXMPPElement.class);
        when(mockElement.getType()).thenReturn(type);
        return mockElement;
    }

    static PartialXMPPElement xmppElement(Type type, String serialization) {
        PartialXMPPElement mockElement = xmppElement(type);
        when(mockElement.serializeCurrentContent()).thenReturn(serialization);
        return mockElement;
    }
}
