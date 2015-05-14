package ar.edu.itba.it.gossip.proxy.xmpp.event;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import ar.edu.itba.it.gossip.proxy.xml.PartialXMLElement;

public abstract class XMPPEvent {
    public abstract XMPPEvent.Type getType();

    public static XMPPEvent from(XMPPEvent.Type type, PartialXMLElement element) {
        switch (type) {
        case AUTH:
            return new AuthStanza(element);
        case AUTH_MECHANISM:
            return new AuthMechanism(element);
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
