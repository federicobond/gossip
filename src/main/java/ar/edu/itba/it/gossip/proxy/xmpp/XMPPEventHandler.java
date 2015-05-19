package ar.edu.itba.it.gossip.proxy.xmpp;

import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

public interface XMPPEventHandler {
    public abstract void handleStart(PartialXMPPElement element);

    public abstract void handleBody(PartialXMPPElement element);

    public abstract void handleEnd(PartialXMPPElement element);
}
