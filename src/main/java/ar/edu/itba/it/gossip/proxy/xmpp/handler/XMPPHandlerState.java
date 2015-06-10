package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

public abstract class XMPPHandlerState<C extends XMPPStreamHandler> {
    private static ProxyConfig PROXY_CONFIG = ProxyConfig.getInstance();

    public abstract void handleStart(C handler, PartialXMPPElement element);

    public abstract void handleBody(C handler, PartialXMPPElement element);

    public abstract void handleEnd(C handler, PartialXMPPElement element);

    public void handleError(C handler, XMLStreamException xmlEx) {
        handler.sendErrorToClient(BAD_FORMAT);
    }

    protected ProxyConfig getProxyConfig() {
        return PROXY_CONFIG;
    }
}
