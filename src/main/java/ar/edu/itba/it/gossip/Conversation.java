package ar.edu.itba.it.gossip;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;

public interface Conversation {

	public void updateSubscription(Selector selector) throws ClosedChannelException;
	
	public void closeChannels();
	
	public String getBufferName(ByteBuffer buffer);
	
}
