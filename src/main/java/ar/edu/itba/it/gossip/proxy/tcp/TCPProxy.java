package ar.edu.itba.it.gossip.proxy.tcp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPChannelEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;

public class TCPProxy implements TCPChannelEventHandler {
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

        /*
         * final SocketChannel originServer = SocketChannel.open();
         * originServer.configureBlocking(false);
         * 
         * // Initiate connection to server and repeatedly poll until complete
         * originServer.connect(new InetSocketAddress(host, port)); originServer
         * .register(key.selector(), SelectionKey.OP_CONNECT, clientChannel);
         */

        reactor.subscribe(clientChannel, this);

        TCPConversation conversation = new TCPConversation(clientChannel);
        conversation.updateSubscription(key.selector());

        /*
         * reactor.subscribe(originServer, this);
         */
    }

    @Override
    public void handleRead(final SelectionKey key) throws IOException {
        TCPConversation conversation = (TCPConversation) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (channel == conversation.getClientChannel()) {
            ByteBuffer buffer = conversation.getReadBufferFor(channel);
            TCPStreamHandler handler = conversation.getHandlerFor(channel);

            int bytesRead = channel.read(buffer);

            // FIXME: just for debugging purposes
            String channelName = conversation.getClientChannel() == channel ? "client"
                    : "origin";
            System.out.println("Read " + bytesRead + " bytes through "
                    + channelName + "Channel (" + channel + ")");
            // FIXME: just for debugging purposes

            if (bytesRead == -1) { // Did the other end close?
                handler.handleEndOfInput();
                finish(conversation);
            } else if (bytesRead > 0) {
                buffer.flip();
                handler.handleRead(buffer,
                        address -> connectToOrigin(key, conversation, address));

                conversation.updateSubscription(key.selector());
            }
        }
    }

    private void connectToOrigin(SelectionKey key,
            TCPConversation conversation, InetSocketAddress address) {
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

    private void finish(TCPConversation conversation) throws IOException {
        // Check!
        reactor.unsubscribe(conversation.getClientChannel());
        reactor.unsubscribe(conversation.getOriginChannel());
        conversation.closeChannels();
    }

    @Override
    public void handleWrite(final SelectionKey key) throws IOException {
        TCPConversation conversation = (TCPConversation) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = conversation.getWriteBufferFor(channel);

        buffer.flip();
        int bytesWritten = channel.write(buffer);

        // FIXME: just for debugging purposes
        String channelName = conversation.getClientChannel() == channel ? "client"
                : "origin";
        System.out.println("Wrote " + bytesWritten + " bytes through "
                + channelName + "Channel (" + channel + ")");
        // FIXME: just for debugging purposes

        buffer.compact(); // Make room for more data to be read in

        conversation.updateSubscription(key.selector());
    }

    @Override
    public void handleConnect(final SelectionKey key) throws IOException {
        TCPConversation conversation = (TCPConversation) key.attachment();
        // conversation.updateSubscription(key.selector());

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

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
