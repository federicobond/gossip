package ar.edu.itba.it.gossip.util.nio;

import static java.nio.charset.StandardCharsets.*;

import java.nio.ByteBuffer;

public abstract class NIOUtils {
    // FIXME: JUST FOR DEBUGGING PURPOSES!
    public static String peek(ByteBuffer buffer) {
        byte[] bytes = buffer.array();
        return new String(bytes, 0, buffer.limit(), UTF_8);
    }
}
