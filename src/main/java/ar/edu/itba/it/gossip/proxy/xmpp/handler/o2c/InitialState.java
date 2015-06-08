package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class InitialState extends HandlerState<OriginToClientXMPPStreamHandler> {
    private static final InitialState INSTANCE = new InitialState();

    protected static InitialState getInstance() {
        return INSTANCE;
    }

    protected InitialState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        assumeType(element, STREAM_START);
        handler.setState(ExpectAuthFeaturesState.getInstance());
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // do nothing, just buffer element's contents
        // TODO: check for potential floods!
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // will never happen
        throw new IllegalStateException("Unexpected state:" + this);
    }

}
