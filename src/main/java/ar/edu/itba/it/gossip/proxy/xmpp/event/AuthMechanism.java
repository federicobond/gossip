package ar.edu.itba.it.gossip.proxy.xmpp.event;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

public class AuthMechanism extends XMPPEvent {
    private final String mechanism;

    AuthMechanism(PartialXMLElement element) {
        this.mechanism = element.getBody();
    }

    public String getMechanism() {
        return mechanism;
    }

    @Override
    public Type getType() {
        return Type.AUTH_MECHANISM;
    }
}
