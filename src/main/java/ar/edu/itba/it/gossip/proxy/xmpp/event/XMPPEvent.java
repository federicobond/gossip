package ar.edu.itba.it.gossip.proxy.xmpp.event;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

public abstract class XMPPEvent {
    public abstract XMPPEvent.Type getType();

    public static XMPPEvent from(XMPPEvent.Type type, PartialXMLElement element) {
        switch (type) {
        case AUTH_CHOICE:
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
        INITIAL, STREAM_START, AUTH_CHOICE, AUTH_FEATURES, AUTH_REGISTER, AUTH_MECHANISMS, AUTH_MECHANISM, AUTH_SUCCESS, AUTH_FAILURE, OTHER
    }
}
