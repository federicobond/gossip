package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

public abstract class XMPPHandlerState<C extends XMPPStreamHandler> {
    private static ProxyConfig PROXY_CONFIG = ProxyConfig.getInstance();

    public abstract void handleStart(C handler, PartialXMPPElement element);

    public abstract void handleBody(C handler, PartialXMPPElement element);

    public abstract void handleEnd(C handler, PartialXMPPElement element);

    protected ProxyConfig getProxyConfig() {
        return PROXY_CONFIG;
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }
}
