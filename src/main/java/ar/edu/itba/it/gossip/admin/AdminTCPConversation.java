package ar.edu.itba.it.gossip.admin;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;

import ar.edu.itba.it.gossip.Conversation;

public class AdminTCPConversation implements Conversation {

	@Override
	public void updateSubscription(Selector selector)
			throws ClosedChannelException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeChannels() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getBufferName(ByteBuffer buffer) {
		// TODO Auto-generated method stub
		return null;
	}

}
