package ar.edu.itba.it.gossip.util.nio;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private final ByteBuffer buf;

    public ByteBufferOutputStream(final ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public void write(int b) throws IOException {
        buf.put((byte) b);
        // FIXME should probably flip / compact / sth!
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        buf.put(bytes, off, len);
        // FIXME should probably flip / compact / sth!
    }

    // FIXME: just here for testing purposes
    public void printBuffer(boolean bytes, boolean flip, boolean escape) {
        BufferUtils.printContent(buf, flip, escape);
        if (bytes) {
            BufferUtils.printContentAsBytes(buf, flip);
        }
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
