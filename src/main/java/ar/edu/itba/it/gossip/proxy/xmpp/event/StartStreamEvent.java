package ar.edu.itba.it.gossip.proxy.xmpp.event;

public class StartStreamEvent extends XMPPEvent {
    @Override
    public Type getType() {
        return Type.START_STREAM;
    }
}
