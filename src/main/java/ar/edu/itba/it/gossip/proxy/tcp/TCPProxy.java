package ar.edu.itba.it.gossip.proxy.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.util.nio.BufferUtils;

public abstract class TCPProxy implements TCPEventHandler {
    private final TCPReactor reactor;

    public TCPProxy(TCPReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handleAccept(final SelectionKey key) throws IOException {
        ServerSocketChannel listenChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = listenChannel.accept();
        clientChannel.configureBlocking(false); // Must be nonblocking to
                                                // register

        reactor.subscribe(clientChannel, this);

        ProxiedTCPConversation conversation = instanceConversation(clientChannel);
        conversation.updateSubscription(key.selector());
    }

    @Override
    public void handleRead(final SelectionKey key) throws IOException {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = conversation.getReadBufferFor(channel);
        TCPStreamHandler handler = conversation.getHandlerFor(channel);

        int bytesRead = channel.read(buffer);

        // FIXME: just for debugging purposes
        String channelName = conversation.getClientChannel() == channel ? "client"
                : "origin";
        String bufferName = conversation.getBufferName(buffer);
        System.out.println("Read " + bytesRead + " bytes into '" + bufferName
                + "' through '" + channelName + "Channel (" + channel + "')");
        // FIXME: just for debugging purposes

        if (bytesRead == -1) { // Did the other end close?
            handler.handleEndOfInput();
            finish(conversation);
        } else if (bytesRead > 0) {
            buffer.flip();

            // FIXME: just for debugging purposes
            System.out.println(bufferName + "'s content: (JUST READ)"
                    + "\n===================\n" + BufferUtils.peek(buffer)
                    + "\n===================\n");
            // FIXME: just for debugging purposes

            int newPosition = handler.handleRead(buffer,
                    address -> connectToOrigin(key, conversation, address));

            buffer.position(newPosition);
            buffer.compact();
            // FIXME: just for debugging purposes
            System.out
                    .println(bufferName
                            + "'s content (AFTER READ):"
                            + buffer
                            + " ("
                            + newPosition
                            + "/"
                            + buffer.capacity()
                            + "="
                            + ((double) newPosition)
                            / buffer.capacity()
                            + ")\n===================\n"
                            + BufferUtils.peek((ByteBuffer) (buffer.duplicate()
                                    .flip())) + "\n===================\n");
            // FIXME: just for debugging purposes

            conversation.updateSubscription(key.selector());
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
        } catch (IOException e) {
            throw new RuntimeException("should inform this to client");
        }
    }

    private void finish(ProxiedTCPConversation conversation) {
        // Check!
        reactor.unsubscribe(conversation.getClientChannel());
        reactor.unsubscribe(conversation.getOriginChannel());
        conversation.closeChannels();
    }

    @Override
    public void handleWrite(final SelectionKey key) throws IOException {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = conversation.getWriteBufferFor(channel);

        // FIXME: just for debugging purposes
        String bufferName = conversation.getBufferName(buffer);
        String channelName = conversation.getClientChannel() == channel ? "client"
                : "origin";
        // FIXME: just for debugging purposes

        buffer.flip();
        // // FIXME: just for debugging purposes
        // System.out.println(bufferName + "'s content: (BEFORE WRITE)"
        // + "\n===================\n" + BufferUtils.peek(buffer)
        // + "\n===================\n");
        // // FIXME: just for debugging purposes

        int bytesWritten = channel.write(buffer);

        System.out.println("Wrote " + bytesWritten + " bytes from '"
                + bufferName + "' through '" + channelName + "Channel ("
                + channel + "')");

        // FIXME: just for debugging purposes
        // System.out.println(bufferName + "'s content: (AFTER WRITE)"
        // + "\n===================\n" + BufferUtils.peek(buffer)
        // + "\n===================\n");
        // FIXME: just for debugging purposes

        buffer.compact(); // Make room for more data to be read in

        conversation.updateSubscription(key.selector());
    }

    @Override
    public void handleConnect(final SelectionKey key) throws IOException {
        ProxiedTCPConversation conversation = (ProxiedTCPConversation) key
                .attachment();

        SocketChannel originChannel = (SocketChannel) key.channel();
        try {
            if (originChannel.finishConnect()) {
                conversation.updateSubscription(key.selector());
            } else {
                finish(conversation);
            }
        } catch (IOException e) {
            System.out.println("Failed to connect to origin server: "
                    + e.getMessage());
            finish(conversation);
        }
    }

    protected abstract ProxiedTCPConversation instanceConversation(
            SocketChannel clientChannel);
}
