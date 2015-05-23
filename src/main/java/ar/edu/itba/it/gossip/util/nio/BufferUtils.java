package ar.edu.itba.it.gossip.util.nio;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.ArrayUtils.subarray;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

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
        return new String(bytes, buffer.position(), buffer.limit()
                - buffer.position(), UTF_8);
    }

    public static void printContentAsBytes(ByteBuffer buffer, boolean flip) {
        ByteBuffer clone = buffer.duplicate();
        if (flip) {
            clone.flip();
        }
        byte[] bytes = clone.array();
        System.out.println("======(bytes)======\n"
                + ArrayUtils.toString(subarray(bytes, buffer.position(),
                        buffer.limit() - buffer.position()))
                + "\n======(bytes)======");
    }

    public static void printContent(ByteBuffer buffer, boolean flip,
            boolean escape) {
        ByteBuffer clone = buffer.duplicate();
        if (flip) {
            clone.flip();
        }
        String str = peek(clone);
        String escaped = escape ? StringEscapeUtils.escapeJava(str) : str;
        System.out.println("======(string)======\n" + escaped
                + "\n======(string)======");
    }
}
