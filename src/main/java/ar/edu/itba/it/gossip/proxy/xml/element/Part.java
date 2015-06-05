package ar.edu.itba.it.gossip.proxy.xml.element;

public abstract class Part {
    private boolean serialized = false;

    boolean isSerialized() {
        return this.serialized;
    }

    String serialize() {
        String serialization = getSerialization();
        this.serialized = true;
        return serialization;
    }

    abstract String getSerialization();
}
