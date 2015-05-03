package ar.edu.itba.it.gossip.proxy.xmpp.event;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public abstract class XMPPEvent {
    public abstract XMPPEvent.Type getType();

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    public enum Type {
        START_STREAM, AUTH, RESPONSE
    }
}
