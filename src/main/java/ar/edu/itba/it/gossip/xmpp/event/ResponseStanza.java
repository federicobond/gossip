package ar.edu.itba.it.gossip.xmpp.event;

import java.util.Map;

public class ResponseStanza extends Event {
    private final String body;

    public ResponseStanza(Map<String, String> attributes, String body) {
        this.body = body;
    }

    @Override
    public Type getType() {
        return Type.RESPONSE;
    }
}
