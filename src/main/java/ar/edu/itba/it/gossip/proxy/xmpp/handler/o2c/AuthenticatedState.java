package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class AuthenticatedState extends HandlerState<OriginToClientXMPPStreamHandler> {
    private static final AuthenticatedState INSTANCE = new AuthenticatedState();

    protected static AuthenticatedState getInstance() {
        return INSTANCE;
    }

    protected AuthenticatedState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        assumeType(element, STREAM_START);
        handler.setState(LinkedState.getInstance());
        System.out
                .println("Origin is linked to the client, now messages may pass freely");

        handler.sendToClient(DOCUMENT_START);
        handler.sendToClient(element); // send stream start to client
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
