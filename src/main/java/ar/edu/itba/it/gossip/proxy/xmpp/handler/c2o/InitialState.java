package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.XMPPUtils.streamFeatures;
import static ar.edu.itba.it.gossip.util.XMPPUtils.streamOpen;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class InitialState extends HandlerState<ClientToOriginXMPPStreamHandler> {
    private static final String PLAIN_AUTH = "PLAIN";

    private static final InitialState INSTANCE = new InitialState();

    protected static InitialState getInstance() {
        return INSTANCE;
    }

    protected InitialState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        assumeType(element, STREAM_START);

        sendStreamOpenToClient(handler);
        handler.sendToClient(streamFeatures(PLAIN_AUTH));

        handler.setState(ExpectCredentialsState.getInstance());
    }

    private void sendStreamOpenToClient(ClientToOriginXMPPStreamHandler handler) {
        handler.sendToClient(DOCUMENT_START + streamOpen());
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // do nothing, just buffer element's contents
        // TODO: check for potential floods!
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        throw new IllegalStateException("Unexpected state: " + this);
    }
}
