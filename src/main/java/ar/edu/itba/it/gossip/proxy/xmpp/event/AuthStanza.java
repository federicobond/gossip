package ar.edu.itba.it.gossip.proxy.xmpp.event;

import java.util.Map;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;

public class AuthStanza extends XMPPEvent {
    private final String mechanism;
    private final Credentials credentials;

    AuthStanza(Map<String, String> attributes, String body) {
        this.mechanism = attributes.get("mechanism");
        this.credentials = Credentials.decode(body);
    }

    @Override
    public Type getType() {
        return Type.AUTH;
    }

    public String getMechanism() {
        return mechanism;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public boolean mechanismMatches(String mechanism) {
        return this.mechanism.equals(mechanism);
    }
}
