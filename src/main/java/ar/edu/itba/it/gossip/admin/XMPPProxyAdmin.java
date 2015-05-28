package ar.edu.itba.it.gossip.admin;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.tcp.TCPConversation;
import ar.edu.itba.it.gossip.proxy.tcp.TCPProxy;

public class XMPPProxyAdmin extends TCPProxy {

	public XMPPProxyAdmin(TCPReactor reactor) {
		super(reactor);
	}

	@Override
	protected AdminTCPConversation instanceConversation(SocketChannel clientChannel) {
		return new AdminProtocolConversation(clientChannel);
	}
	
	@Override
    public String toString() {
        return reflectionToString(this);
    }

}
