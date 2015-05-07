package ar.edu.itba.it.gossip.proxy.xmpp.event;

class GenericXMPPEvent extends XMPPEvent {
    private final XMPPEvent.Type type;

    public GenericXMPPEvent(final XMPPEvent.Type type) {
        this.type = type;
    }

    @Override
    public XMPPEvent.Type getType() {
        return type;
    }
}
