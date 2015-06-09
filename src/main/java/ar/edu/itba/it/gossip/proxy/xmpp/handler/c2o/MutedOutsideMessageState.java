package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class MutedOutsideMessageState extends
        XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
    private static final MutedOutsideMessageState INSTANCE = new MutedOutsideMessageState();

    protected static MutedOutsideMessageState getInstance() {
        return INSTANCE;
    }

    protected MutedOutsideMessageState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == MESSAGE) {
            handler.setClientNotifiedOfMute(false);
            handler.setState(MutedInMessageState.getInstance());
        }
        handler.sendToOrigin(element);
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToOrigin(element);
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToOrigin(element);
    }
}
