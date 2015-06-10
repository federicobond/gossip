package ar.edu.itba.it.gossip.proxy.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.it.gossip.async.tcp.TCPEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;

public abstract class TCPProxy implements TCPEventHandler {
    private final Logger logger = LoggerFactory.getLogger(TCPProxy.class);

    private final TCPReactor reactor;
    private final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    public TCPProxy(TCPReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handleAccept(final SelectionKey key) {
        try {
            ServerSocketChannel listenChannel = (ServerSocketChannel) key
                    .channel();
            SocketChannel clientChannel = listenChannel.accept();
            clientChannel.configureBlocking(false); // Must be nonblocking to
                                                    // register

            reactor.subscribe(clientChannel, this);

            ProxiedTCPConversation conversation = instanceConversation(clientChannel);
            conversation.updateSubscription(key.selector());
            proxyConfig.countAccess();
        } catch (Exception ex) {
            logger.error(this + ": could not accept", ex);
        }
    }

    @Override
    public void handleRead(final SelectionKey key) {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            ByteBuffer buffer = conversation.getReadBufferFor(channel);
            TCPStreamHandler handler = conversation.getHandlerFor(channel);

            int bytesRead = channel.read(buffer);
            proxyConfig.countReads(bytesRead);

            if (bytesRead == -1) { // Did the other end close?
                handler.handleEndOfInput();
                finish(conversation);
            } else if (bytesRead > 0) {
                buffer.flip();

                handler.handleRead(buffer,
                        address -> connectToOrigin(key, conversation, address));

                conversation.updateSubscription(key.selector());
            }
        } catch (Exception ex) {
            logger.error(this + ": error on read", ex);
            finish(conversation);
        }
    }

    private void connectToOrigin(SelectionKey key,
            ProxiedTCPConversation conversation, InetSocketAddress address) {
        try {
            SocketChannel origin = SocketChannel.open();
            origin.configureBlocking(false);
            origin.connect(address);

            reactor.subscribe(origin, this);
            conversation.connectToOrigin(origin);
        } catch (Exception ex) {
            logger.error(this + ": could not connect to origin server", ex);
            finish(conversation);
        }
    }

    private void finish(ProxiedTCPConversation conversation) {
        try {
            reactor.unsubscribe(conversation.getClientChannel());
            reactor.unsubscribe(conversation.getOriginChannel());
            conversation.closeChannels();
        } catch (Exception ex) {
            logger.error(this + ": could not finish conversation", ex);
            finish(conversation);
        }
    }

    @Override
    public void handleWrite(final SelectionKey key) {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            ByteBuffer buffer = conversation.getWriteBufferFor(channel);

            buffer.flip();

            int bytesWritten = channel.write(buffer);
            proxyConfig.countWrites(bytesWritten);

            buffer.compact(); // Make room for more data to be read in

            conversation.updateSubscription(key.selector());
        } catch (Exception ex) {
            logger.error(this + ": error on write", ex);
            finish(conversation);
        }
    }

    @Override
    public void handleConnect(final SelectionKey key) {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();
        try {
            SocketChannel originChannel = (SocketChannel) key.channel();
            if (originChannel.finishConnect()) {
                conversation.updateSubscription(key.selector());
            } else {
                finish(conversation);
            }
        } catch (Exception ex) {
            logger.error(this + ": failed to connect to origin server", ex);
            finish(conversation);
        }
    }

    protected abstract ProxiedTCPConversation instanceConversation(
            SocketChannel clientChannel);

    protected void closeAfterTimeout(SocketChannel channel, long millis) {
        reactor.closeAfterTimeout(channel, millis);
    }
}
