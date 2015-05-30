package ar.edu.itba.it.gossip.util;

public interface PartiallySerializable {
    String serializeCurrentContent();

    default void consumeCurrentContent() {
        serializeCurrentContent(); // note that serializing will just drop the
                                   // elements as expected
    }
}
