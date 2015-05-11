package ar.edu.itba.it.gossip.proxy.xmpp.event;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.util.Map;

public abstract class XMPPEvent {
    public abstract XMPPEvent.Type getType();

    public static XMPPEvent from(XMPPEvent.Type type,
            Map<String, String> attributes, String body) {
        switch (type) {
        case AUTH:
            return new AuthStanza(attributes, body);
        case AUTH_MECHANISM:
            return new AuthMechanism(attributes, body);
        default:
            return new GenericXMPPEvent(type);
        }
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    public enum Type {
        START_STREAM, AUTH, RESPONSE, AUTH_REGISTER, AUTH_MECHANISMS, AUTH_MECHANISM, AUTH_FEATURES_END, AUTH_SUCCESS, AUTH_FAILURE
    }
}
