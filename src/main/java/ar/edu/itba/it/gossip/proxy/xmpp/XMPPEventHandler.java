package ar.edu.itba.it.gossip.proxy.xmpp;

import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

public interface XMPPEventHandler {
    void handleStart(PartialXMPPElement element);

    void handleBody(PartialXMPPElement element);

    void handleEnd(PartialXMPPElement element);
}
