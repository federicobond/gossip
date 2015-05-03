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
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        buf.put(bytes, off, len);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
