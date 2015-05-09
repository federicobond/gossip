package ar.edu.itba.it.gossip.util.nio;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;

public abstract class BufferUtils {
    // // this method is quite lenient in the sense that it will not fail on
    // // "empty" copies (it will just do nothing)
    // public static void transfer(ByteBuffer from, ByteBuffer to) {
    // // TODO: check!
    // if (!to.hasRemaining() || !from.hasRemaining()) {
    // // either toBuffer is full or fromBuffer has nothing copyable
    // return;
    // }
    // int toCopy = min(from.limit(), to.remaining() - from.remaining());
    // to.put(from.array(), from.position(), toCopy);
    // int newLimit = min(from.limit() + toCopy, from.capacity());
    // from.limit(newLimit);
    // from.compact();
    // }

    // FIXME: JUST FOR DEBUGGING PURPOSES!
    public static String peek(ByteBuffer buffer) {
        byte[] bytes = buffer.array();
        // TODO: check!
        return new String(bytes, buffer.position(), buffer.limit(), UTF_8);
    }
}
