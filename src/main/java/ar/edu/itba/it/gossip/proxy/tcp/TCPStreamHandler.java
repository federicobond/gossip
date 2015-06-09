package ar.edu.itba.it.gossip.proxy.tcp;

import java.nio.ByteBuffer;

//NOTE: A handler is activated only when data is written into its stream
// (that is, on the TCP *read* event of the stream's input channel)
public interface TCPStreamHandler {
    void handleEndOfInput();

    void handleRead(ByteBuffer buf, DeferredConnector connector);

    void handleError(Exception ex);
}
