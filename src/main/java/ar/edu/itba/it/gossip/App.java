package ar.edu.itba.it.gossip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ar.edu.itba.it.gossip.tcp.TCPHandler;
import ar.edu.itba.it.gossip.tcp.TCPReactor;
import ar.edu.itba.it.gossip.tcp.TCPReactorImpl;

public class App {
    public static void main(String[] args) throws IOException {
        Map<Integer, TCPHandler> protocolHandlers = new HashMap<>();

        TCPReactor reactor = new TCPReactorImpl("localhost", protocolHandlers);
        reactor.start();
    }
}
