package ar.edu.itba.it.gossip.admin;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.it.gossip.async.tcp.TCPEventHandler;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;

public class XMPPProxyAdmin implements TCPEventHandler {

    private final TCPReactor reactor;

    public XMPPProxyAdmin(final TCPReactor reactor) {
        this.reactor = reactor;
    }

    @Override
    public void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel listenChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = listenChannel.accept();
        clientChannel.configureBlocking(false); // Must be nonblocking to
                                                // register

        reactor.subscribe(clientChannel, this);

        // TODO: change this
        // clientChannel.register(key.selector(), SelectionKey.OP_READ
        // | SelectionKey.OP_WRITE, null);
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        // reactor.unsubscribe((SocketChannel) key.channel());
    }

    @Override
    public void handleWrite(SelectionKey key) throws IOException {
        // reactor.unsubscribe((SocketChannel) key.channel());
    }

    @Override
    public void handleConnect(SelectionKey key) throws IOException {
        // should do nothing, since this handler will never connect to sockets
    }
}
