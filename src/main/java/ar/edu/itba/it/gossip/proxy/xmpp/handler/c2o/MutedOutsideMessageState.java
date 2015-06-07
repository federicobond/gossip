package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

class MutedOutsideMessageState extends HandlerState {
    private static final MutedOutsideMessageState INSTANCE = new MutedOutsideMessageState();

    protected static MutedOutsideMessageState getInstance() {
        return INSTANCE;
    }

    protected MutedOutsideMessageState() {
    }

    @Override
    protected void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == MESSAGE) {
            handler.setClientNotifiedOfMute(false);
            handler.setState(MutedInMessageState.getInstance());
        }
        sendToOrigin(handler, element);
    }

    @Override
    protected void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        sendToOrigin(handler, element);
    }

    @Override
    protected void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        sendToOrigin(handler, element);
    }
}
