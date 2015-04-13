package ar.edu.itba.it.gossip;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import ar.edu.itba.it.gossip.tcp.TCPHandler;
import ar.edu.itba.it.gossip.tcp.TCPProxy;
import ar.edu.itba.it.gossip.tcp.TCPReactor;
import ar.edu.itba.it.gossip.tcp.TCPReactorImpl;

public class Example {
    public static void main(String[] args) throws IOException {
        short proxyPort = 9998;
        short originPort = 9999;

        Map<Integer, TCPHandler> protocolHandlersByPort = Collections
                .singletonMap((int) proxyPort, new TCPProxy(originPort));

        TCPReactor reactor = new TCPReactorImpl(protocolHandlersByPort);
        reactor.start();
    }
}
