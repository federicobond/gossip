package ar.edu.itba.it.gossip.util.nio;

import static java.lang.Math.min;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buf;

    public ByteBufferInputStream(final ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        //FIXME should probably flip / compact / sth!
        return buf.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }

        len = min(len, buf.remaining());
        buf.get(bytes, off, len);
        //FIXME should probably flip / compact / sth!
        return len;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
