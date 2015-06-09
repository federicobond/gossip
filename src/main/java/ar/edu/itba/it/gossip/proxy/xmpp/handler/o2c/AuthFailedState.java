package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class AuthFailedState extends XMPPHandlerState<OriginToClientXMPPStreamHandler> {
    private static final AuthFailedState INSTANCE = new AuthFailedState();

    protected static AuthFailedState getInstance() {
        return INSTANCE;
    }

    protected AuthFailedState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToClient(element);
        // TODO: should terminate the connection or something here!
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToClient(element);
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToClient(element);
    }
}
