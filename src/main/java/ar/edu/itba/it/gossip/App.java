package ar.edu.itba.it.gossip;

import java.io.IOException;

import ar.edu.itba.it.gossip.admin.XMPPProxyAdmin;
import ar.edu.itba.it.gossip.async.tcp.TCPReactor;
import ar.edu.itba.it.gossip.async.tcp.TCPReactorImpl;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPProxy;

public class App {
    public static void main(String[] args) throws IOException {
        TCPReactor reactor = new TCPReactorImpl();
        
        short proxyPort = 9998;
        short adminPort = 9999;
        
        reactor.addHandler(new XMPPProxy(reactor), proxyPort); 
        reactor.addHandler(new XMPPProxyAdmin(reactor), adminPort);

        
        reactor.start();
    }
}
