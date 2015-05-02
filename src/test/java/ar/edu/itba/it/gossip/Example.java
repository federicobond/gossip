package ar.edu.itba.it.gossip;

import java.io.IOException;

import ar.edu.itba.it.gossip.tcp.*;

public class Example {
    public static void main(String[] args) throws IOException {
        short proxyPort = 9998;
        short originPort = 5222;

        TCPReactor reactor = new TCPReactorImpl();
        reactor.addHandler(new TCPXMLProxy3(reactor, originPort), proxyPort);

        reactor.start();
    }
}
