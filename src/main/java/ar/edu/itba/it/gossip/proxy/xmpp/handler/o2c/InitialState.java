package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
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
        if (element.getType() != STREAM_START) {
            handler.sendErrorToClient(BAD_FORMAT);
        }
        handler.setState(ExpectAuthFeaturesState.getInstance());
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        element.consumeCurrentContent();
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // will never happen
        throw new RuntimeException();
    }
}
