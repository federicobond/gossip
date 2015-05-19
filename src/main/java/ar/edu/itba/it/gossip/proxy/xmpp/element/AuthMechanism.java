package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

public class AuthMechanism extends PartialXMPPElement {
    private final String mechanism;

    AuthMechanism(final PartialXMLElement element) {
        super(element);
        this.mechanism = element.getBody();
    }

    public String getMechanism() {
        return mechanism;
    }
}
