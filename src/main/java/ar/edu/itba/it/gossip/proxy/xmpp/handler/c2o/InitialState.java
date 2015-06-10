package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.streamFeatures;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.streamOpen;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class InitialState extends XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
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
        if (element.getType() != STREAM_START) {
            sendStreamOpenToClient(handler);
            handler.sendErrorToClient(BAD_FORMAT);
            return;
        }

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
        element.consumeCurrentContent();
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // will never happen
        throw new RuntimeException();
    }
}
