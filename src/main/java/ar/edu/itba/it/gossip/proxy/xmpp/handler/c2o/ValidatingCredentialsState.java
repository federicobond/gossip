package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class ValidatingCredentialsState extends
        XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
    private static final ValidatingCredentialsState INSTANCE = new ValidatingCredentialsState();

    protected static ValidatingCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ValidatingCredentialsState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() != STREAM_START) {
            handler.sendErrorToClient(BAD_FORMAT);
        }

        handler.sendToOrigin(DOCUMENT_START);
        handler.sendToOrigin(element); // send client's stream start to origin

        handler.setState(LinkedState.getInstance());
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        element.consumeCurrentContent();
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == STREAM_START) {
            handler.sendErrorToClient(BAD_FORMAT);
            return;
        }
        throw new RuntimeException(); // will never happen
    }
}
