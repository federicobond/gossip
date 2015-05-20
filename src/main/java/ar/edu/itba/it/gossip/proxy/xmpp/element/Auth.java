package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;

public class Auth extends PartialXMPPElement {
    private Credentials credentials;

    Auth(final PartialXMLElement element) {
        super(element);
    }

    public Credentials getCredentials() {
        if (credentials == null) {
            credentials = Credentials.decode(getXML().getBody());
        }
        return credentials;
    }
}
