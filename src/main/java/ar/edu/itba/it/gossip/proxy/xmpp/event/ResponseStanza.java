package ar.edu.itba.it.gossip.proxy.xmpp.event;

import java.util.Map;

public class ResponseStanza extends XMPPEvent {
    private final String body;

    public ResponseStanza(Map<String, String> attributes, final String body) {
        this.body = body;
    }

    @Override
    public Type getType() {
        return Type.RESPONSE;
    }
}
