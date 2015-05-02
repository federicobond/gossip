package ar.edu.itba.it.gossip.xmpp.event;

public class StartStreamEvent extends Event {
    @Override
    public Type getType() {
        return Type.START_STREAM;
    }
}
