package ar.edu.itba.it.gossip.proxy.tcp.stream;

import java.io.InputStream;
import java.io.OutputStream;

class ByteStreamHandle extends ByteStream {
    private final ByteStream stream;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public ByteStreamHandle(final ByteStream stream) {
        this.stream = stream;
        this.inputStream = stream.getInputStream();
        this.outputStream = stream.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void flush() {
        stream.flush();
    }
}
