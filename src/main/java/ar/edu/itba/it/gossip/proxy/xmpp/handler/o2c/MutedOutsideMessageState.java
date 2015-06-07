package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class MutedOutsideMessageState extends
        HandlerState<OriginToClientXMPPStreamHandler> {
    private static final MutedOutsideMessageState INSTANCE = new MutedOutsideMessageState();

    protected static MutedOutsideMessageState getInstance() {
        return INSTANCE;
    }

    protected MutedOutsideMessageState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == MESSAGE) {
            handler.setState(MutedInMessageState.getInstance());
        }
        handler.sendToClient(element);
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
