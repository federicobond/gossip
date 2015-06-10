package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class AuthenticatedState extends
        XMPPHandlerState<OriginToClientXMPPStreamHandler> {
    private static final AuthenticatedState INSTANCE = new AuthenticatedState();

    protected static AuthenticatedState getInstance() {
        return INSTANCE;
    }

    protected AuthenticatedState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() != STREAM_START) {
            handler.sendErrorToClient(BAD_FORMAT);
        }

        handler.setState(LinkedState.getInstance());

        handler.sendToClient(DOCUMENT_START);
        handler.sendToClient(element); // send stream start to client
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        element.consumeCurrentContent();
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        throw new RuntimeException(); // will never happen
    }
}
