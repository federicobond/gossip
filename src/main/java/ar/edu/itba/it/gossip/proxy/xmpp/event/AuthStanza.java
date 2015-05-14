package ar.edu.itba.it.gossip.proxy.xmpp.event;

import ar.edu.itba.it.gossip.proxy.xml.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;

public class AuthStanza extends XMPPEvent {
    private final String mechanism;
    private final Credentials credentials;

    AuthStanza(PartialXMLElement element) {
        this.mechanism = element.getAttributes().get("mechanism");
        this.credentials = Credentials.decode(element.getBody());
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
