package ar.edu.itba.it.gossip.util.nio;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;

public abstract class BufferUtils {
    /*
     * this method is quite lenient: <ul> <li> it will not fail on "empty"
     * copies (it will just do nothing)</li> <li> it will try to copy as much as
     * will fit on the other side </it> </ul>
     */
    public static void transfer(ByteBuffer from, ByteBuffer to) {
        // TODO: check!
        if (!to.hasRemaining() || !from.hasRemaining()) {
            // either toBuffer is full or fromBuffer has nothing copyable
            return;
        }
        int toTransfer = min(from.remaining(),
                to.remaining() - from.remaining());

        // FIXME: just for debugging purposes!
        String toTransferStr = new String(from.array(), from.position(),
                toTransfer);
        System.out.println("Transferring " + toTransfer
                + " bytes\n===================\n" + toTransferStr
                + "\n===================\nfrom " + from + " to " + to);
        // FIXME: just for debugging purposes!

        to.put(from.array(), from.position(), toTransfer);
        to.flip();

        int newPosition = min(from.position() + toTransfer, from.limit());
        from.position(newPosition);
        from.compact();
    }

    // FIXME: JUST FOR DEBUGGING PURPOSES!
    public static String peek(ByteBuffer buffer) {
        byte[] bytes = buffer.array();
        // TODO: check!
        try {
            return new String(bytes, buffer.position(), buffer.limit(), UTF_8);
        } catch (StringIndexOutOfBoundsException ex) {
            return ex.toString();
        }
    }
}
