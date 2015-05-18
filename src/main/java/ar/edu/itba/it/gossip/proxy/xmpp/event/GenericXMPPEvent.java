package ar.edu.itba.it.gossip.proxy.xmpp.event;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

class GenericXMPPEvent extends XMPPEvent {
    private final XMPPEvent.Type type;

    public GenericXMPPEvent(final Type type, final PartialXMLElement element) {
        super(element);
        this.type = type;
    }

    @Override
    public XMPPEvent.Type getType() {
        return type;
    }
}
