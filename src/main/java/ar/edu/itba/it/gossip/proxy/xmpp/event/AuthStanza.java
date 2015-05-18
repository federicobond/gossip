package ar.edu.itba.it.gossip.proxy.xmpp.event;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;

public class AuthStanza extends XMPPEvent {
    private final Credentials credentials;

    AuthStanza(final PartialXMLElement element) {
        super(element);
        this.credentials = Credentials.decode(element.getBody());
    }

    @Override
    public Type getType() {
        return Type.AUTH_CHOICE;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
