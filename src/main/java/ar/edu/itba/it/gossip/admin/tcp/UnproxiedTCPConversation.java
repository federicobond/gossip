package ar.edu.itba.it.gossip.admin.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;
import ar.edu.itba.it.gossip.util.nio.TCPConversation;

public class UnproxiedTCPConversation implements TCPConversation {
    private final TCPStream stream;
    private boolean hasQuit = false;

    protected UnproxiedTCPConversation(SocketChannel channel) {
        this.stream = new TCPStream(channel, channel);
    }

    @Override
    public void updateSubscription(Selector selector)
            throws ClosedChannelException {
        int streamFlags = stream.getFromSubscriptionFlags();
        int streamToFlags = stream.getToSubscriptionFlags();
        stream.getFromChannel().register(selector, streamFlags | streamToFlags, this);
        //stream.getToChannel().register(selector, streamToFlags, this);
    }

    @Override
    public void closeChannels() {
        try {
            stream.getFromChannel().close();
        } catch (IOException ignore) {
        }
    }

    public void quit() {
        hasQuit = true;
    }

    public boolean hasQuit() {
        return hasQuit;
    }

    public TCPStream getStream() {
        return stream;
    }
    
    public ByteBuffer getReadBuffer(){
        return stream.getFromBuffer();
    }
    
    public ByteBuffer getWriteBuffer(){
        return stream.getToBuffer();
    }
    
    public TCPStreamHandler getHandler(){
        return stream.getHandler();
    }
    
    // FIXME: just for debugging purposes
    public String getBufferName(ByteBuffer buffer){
        if(buffer == stream.getFromBuffer()){
            return "from buffer";
        }
        if(buffer == stream.getToBuffer()){
            return "to buffer";
        }
        throw new IllegalArgumentException("Unknown buffer");
    }
    
    public SocketChannel getChannel(){
        return stream.getFromChannel();
    }
}
