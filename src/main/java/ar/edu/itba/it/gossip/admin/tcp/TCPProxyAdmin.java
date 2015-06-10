package ar.edu.itba.it.gossip.admin.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.it.gossip.async.tcp.TCPEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.util.nio.BufferUtils;

public abstract class TCPProxyAdmin implements TCPEventHandler {
    private final Logger logger = LoggerFactory.getLogger(TCPProxyAdmin.class);

    private final TCPReactor reactor;

    public TCPProxyAdmin(final TCPReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handleAccept(SelectionKey key) {
        try {
            ServerSocketChannel listenChannel = (ServerSocketChannel) key
                    .channel();
            SocketChannel channel = listenChannel.accept();
            channel.configureBlocking(false); // Must be nonblocking to
                                              // register

            reactor.subscribe(channel, this);

            UnproxiedTCPConversation conversation = instanceConversation(channel);
            conversation.updateSubscription(key.selector());
        } catch (Exception ex) {
            logger.error(this + ": could not accept", ex);
        }
    }

    @Override
    public void handleRead(SelectionKey key) {
        // reactor.unsubscribe((SocketChannel) key.channel());
        SocketChannel channel = (SocketChannel) key.channel();
        UnproxiedTCPConversation conversation = (UnproxiedTCPConversation) key
                .attachment();
        try {

            ByteBuffer buffer = conversation.getReadBuffer();
            TCPStreamHandler handler = conversation.getHandler();

            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) {
                handler.handleEndOfInput();
                finish(conversation);
            } else if (bytesRead > 0) {
                buffer.flip();

                handler.handleRead(buffer,
                        address -> connect(key, conversation, address));

                conversation.updateSubscription(key.selector());
            }
        } catch (Exception ex) {
            logger.error(this + ": error on read", ex);
            finish(conversation);
        }
    }

    private void connect(SelectionKey key,
            UnproxiedTCPConversation conversation, InetSocketAddress address) {
        try {
            SocketChannel origin = SocketChannel.open();
            origin.configureBlocking(false);
            origin.connect(address);

            reactor.subscribe(origin, this);
        } catch (Exception ex) {
            logger.error(this + ": could not connect", ex);
            finish(conversation);
        }
    }

    private void finish(UnproxiedTCPConversation conversation) {
        reactor.unsubscribe(conversation.getChannel());
        conversation.closeChannels();
    }

    @Override
    public void handleWrite(SelectionKey key) {
        UnproxiedTCPConversation conversation = (UnproxiedTCPConversation) key
                .attachment();
        try {
            ByteBuffer buffer = conversation.getWriteBuffer();
            buffer.flip();

            buffer.compact(); // Make room for more data to be read in

            conversation.updateSubscription(key.selector());

            if (conversation.hasQuit()) {
                conversation.closeChannels();
            }
        } catch (Exception ex) {
            logger.error(this + ": error on write", ex);
            finish(conversation);
        }
    }

    @Override
    public void handleConnect(SelectionKey key) {
        // should do nothing, since this handler will never connect to sockets
    }

    protected abstract UnproxiedTCPConversation instanceConversation(
            SocketChannel channel);

    protected void closeAfterTimeout(SocketChannel channel, long millis) {
        reactor.closeAfterTimeout(channel, millis);
    }
}
