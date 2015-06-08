package ar.edu.itba.it.gossip.proxy.tcp.stream;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class ByteStream {
    public abstract InputStream getInputStream();

    public abstract OutputStream getOutputStream();

    public abstract void flush();

    public abstract void pauseInflow();

    public abstract void resumeInflow();

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
