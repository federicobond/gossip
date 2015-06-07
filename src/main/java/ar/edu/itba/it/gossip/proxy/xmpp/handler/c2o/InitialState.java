package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.XMPPUtils.streamFeatures;
import static ar.edu.itba.it.gossip.util.XMPPUtils.streamOpen;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

class InitialState extends HandlerState {
    private static final String PLAIN_AUTH = "PLAIN";

    private static final InitialState INSTANCE = new InitialState();

    protected static InitialState getInstance() {
        return INSTANCE;
    }

    protected InitialState() {
    }

    @Override
    protected void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        assumeType(element, STREAM_START);

        sendStreamOpenToClient(handler);
        sendToClient(handler, streamFeatures(PLAIN_AUTH));

        handler.setState(ExpectCredentialsState.getInstance());
    }

    private void sendStreamOpenToClient(ClientToOriginXMPPStreamHandler handler) {
        sendToClient(handler, DOCUMENT_START + streamOpen());
    }

    @Override
    protected void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // do nothing, just buffer element's contents
        // TODO: check for potential floods!
    }

    @Override
    protected void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        throw new IllegalStateException("Unexpected state: " + this);
    }
}
