package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class Auth extends PartialXMPPElement {
    private Credentials credentials;

    public Auth(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    public Credentials getCredentials() {
        if (credentials == null) {
            credentials = Credentials.decode(getBody());
        }
        return credentials;
    }
}
