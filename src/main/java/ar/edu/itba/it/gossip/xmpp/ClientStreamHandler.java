package ar.edu.itba.it.gossip.xmpp;

import ar.edu.itba.it.gossip.xmpp.event.AuthStanza;
import ar.edu.itba.it.gossip.xmpp.event.Event;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static ar.edu.itba.it.gossip.xmpp.ClientStreamHandler.AuthState.*;

public class ClientStreamHandler extends StreamHandler {
    enum AuthState {
        INITIAL, NEGOTIATING, AUTHENTICATING, AUTHENTICATED, OPEN, CONFIRMED
    }

    private String username;
    private String password;

    private AuthState authState = AuthState.INITIAL;

    public ClientStreamHandler(OutputStream client) throws XMLStreamException {
        super();

        reader = INPUT_FACTORY.createAsyncFor(ByteBuffer.allocate(0));

        // this.client = client;
        this.client = client;

        setEventHandler(new StanzaEventHandler(this));
    }

    public void sendStreamOpen() {
        try {
            client.write("<?xml version=\"1.0\"?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' version='1.0' from='localhost' id='6e5bb830-1e2d-40c3-8ebf-eacec740d83b' xml:lang='en' xmlns='jabber:client'>"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendStreamFeatures() {
        try {
            client.write(("<stream:features>\n" +
                    "<register xmlns=\"http://jabber.org/features/iq-register\"/>\n" +
                    "<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">\n" +
                    "<mechanism>PLAIN</mechanism>\n" +
                    "</mechanisms>\n" +
                    "</stream:features>").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAuthenticatedStreamFeatures() {
        try {
            client.write(("<stream:features>\n" +
                    "<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">\n" +
                    "<required/>\n" +
                    "</bind>\n" +
                    "<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\">\n" +
                    "<optional/>\n" +
                    "</session>\n" +
                    "<ver xmlns=\"urn:xmpp:features:rosterver\"/>\n" +
                    "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"http://prosody.im\" ver=\"ZBWApSGFMsTZkuVThHtyU5xv1Mk=\" hash=\"sha-1\"/>\n" +
                    "</stream:features>").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAuthChallenge() {
        try {
            client.write("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">cmVhbG09ImxvY2FsaG9zdCIsbm9uY2U9IjA4NmQzNTJmLTY3YWMtNDU0MS1iNzA0LTQ1MmQ5ZjViNjRmMiIscW9wPSJhdXRoIixjaGFyc2V0PXV0Zi04LGFsZ29yaXRobT1tZDUtc2Vzcw==</challenge>"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAuthChallengeResponse() {
        try {
            client.write("<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">cnNwYXV0aD1lZTAwMzkyZDlhYTg2NmYzMDFhM2U0MjI4MzFkYTYwOQ==</challenge>"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAuthSuccess() {
        try {
            client.write("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processEvent(Event event) {
        switch (authState) {
            case INITIAL:
                matchEventType(event, Event.Type.START_STREAM);
                sendStreamOpen();
                sendStreamFeatures();
                authState = NEGOTIATING;
                break;
            case NEGOTIATING:
                matchEventType(event, Event.Type.AUTH);
                AuthStanza auth = (AuthStanza) event;
                matchAuthMechanism(auth, "PLAIN");
                username = auth.getUsername();
                password = auth.getPassword();
                sendAuthChallenge();
                authState = AUTHENTICATING;
                break;
            case AUTHENTICATING:
                matchEventType(event, Event.Type.RESPONSE);
                sendAuthChallengeResponse();
                authState = AUTHENTICATED;
                break;
            case AUTHENTICATED:
                matchEventType(event, Event.Type.RESPONSE);
                sendAuthSuccess();
                authState = CONFIRMED;
                resetStream();
                break;
            case CONFIRMED:
                matchEventType(event, Event.Type.START_STREAM);
                sendStreamOpen();
                sendAuthenticatedStreamFeatures();
                System.out.println(username);
                System.out.println(password);
                authState = OPEN;
                break;
            case OPEN:
                break;
        }

    }

    private void matchAuthMechanism(AuthStanza auth, String mechanism) {
        if (!auth.getMechanism().equals(mechanism)) {
            throw new RuntimeException("auth mechanism not supported: " + mechanism);
        }
    }

    private void matchEventType(Event event, Event.Type type) {
        if (event.getType() != type) {
            throw new RuntimeException("event type mismatch");
        }
    }

}
