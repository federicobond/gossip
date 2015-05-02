package ar.edu.itba.it.gossip.xmpp.event;

public abstract class Event {

    public abstract Event.Type getType();

    public enum Type {
        START_STREAM, AUTH, RESPONSE
    }
}
