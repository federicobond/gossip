package ar.edu.itba.it.gossip.admin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.Conversation;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;

public class AdminTCPConversation implements Conversation {
    
    private final TCPStream stream;
        
    protected AdminTCPConversation(SocketChannel channel) {
        this.stream = new TCPStream(channel, channel);
    }
    
	@Override
	public void updateSubscription(Selector selector)
			throws ClosedChannelException {
	    int streamFlags = stream.getFromSubscriptionFlags();
        stream.getFromChannel().register(selector, streamFlags, this);
	}

	@Override
	public void closeChannels() {
	    try {
	        stream.getFromChannel().close();
        } catch (IOException ignore) {
        }
		
	}

	@Override
	public String getBufferName(ByteBuffer buffer) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public TCPStream getStream(){
	    return stream;
	}

}
