package ar.edu.itba.it.gossip;

import java.io.IOException;

import ar.edu.itba.it.gossip.tcp.TCPProxy;
import ar.edu.itba.it.gossip.tcp.TCPReactor;
import ar.edu.itba.it.gossip.tcp.TCPReactorImpl;

public class Example {
    public static void main(String[] args) throws IOException {
        short proxyPort = 9998;
        short originPort = 9999;

        TCPReactor reactor = new TCPReactorImpl();
        reactor.addHandler(new TCPProxy(reactor, originPort), proxyPort);

        reactor.start();
    }
}
