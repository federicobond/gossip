package ar.edu.itba.it.gossip.proxy.xmpp.event;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

public abstract class XMPPEvent {
    public static XMPPEvent from(Type type, PartialXMLElement element) {
        switch (type) {
        case AUTH_CHOICE:
            return new AuthStanza(element);
        case AUTH_MECHANISM:
            return new AuthMechanism(element);
        default:
            return new GenericXMPPEvent(type, element);
        }
    }

    private final PartialXMLElement element;

    XMPPEvent(final PartialXMLElement element) {
        this.element = element;
    }

    public abstract Type getType();

    public PartialXMLElement getElement() {
        return element;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    public enum Type {
        STREAM_START, AUTH_CHOICE, AUTH_FEATURES, AUTH_REGISTER, AUTH_MECHANISMS, AUTH_MECHANISM, AUTH_SUCCESS, AUTH_FAILURE, OTHER
    }
}
