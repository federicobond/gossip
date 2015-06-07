package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

abstract class HandlerState {
    private static ProxyConfig PROXY_CONFIG = ProxyConfig.getInstance();

    protected abstract void handleStart(
            ClientToOriginXMPPStreamHandler handler, PartialXMPPElement element);

    protected abstract void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element);

    protected abstract void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element);

    protected void sendToClient(ClientToOriginXMPPStreamHandler handler,
            String text) {
        handler.sendToClient(text);
    }

    protected void sendToOrigin(ClientToOriginXMPPStreamHandler handler,
            String text) {
        handler.sendToOrigin(text);
    }

    protected void sendToOrigin(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // System.out.println("\n<C2O sending to origin>");
        String currentContent = element.serializeCurrentContent();
        // System.out.println("Message:\n'"
        // + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
        // + ArrayUtils.toString(currentContent.getBytes()));
        sendToOrigin(handler, currentContent);
        // System.out.println("\nOutgoing buffer afterwards:");
        // ((ByteBufferOutputStream) toOrigin).printBuffer(false, true, true);
        // System.out.println("</C2O sending to origin>\n");
    }

    protected ProxyConfig getProxyConfig() {
        return PROXY_CONFIG;
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }
}
