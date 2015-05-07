package ar.edu.itba.it.gossip.proxy.xmpp.event;

import java.util.Map;

public class AuthMechanism extends XMPPEvent {
    private final String mechanism;

    public AuthMechanism(Map<String, String> attributes, String body) {
        this.mechanism = body;
    }

    public String getMechanism() {
        return mechanism;
    }

    @Override
    public Type getType() {
        return Type.AUTH_MECHANISM;
    }

}
