package ar.edu.itba.it.gossip;

import java.io.IOException;

import ar.edu.itba.it.gossip.tcp.TCPReactor;
import ar.edu.itba.it.gossip.tcp.TCPReactorImpl;

public class App {
    public static void main(String[] args) throws IOException {
        TCPReactor reactor = new TCPReactorImpl("localhost");

        reactor.start();
    }
}
