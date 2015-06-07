package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_START;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class ValidatingCredentialsState extends
        HandlerState<ClientToOriginXMPPStreamHandler> {
    private static final ValidatingCredentialsState INSTANCE = new ValidatingCredentialsState();

    protected static ValidatingCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ValidatingCredentialsState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // FIXME: do check that the credentials were actually valid! (the
        // code here is just assuming the client will behave and wait for an
        // auth <success>).
        assumeType(element, STREAM_START);

        handler.setState(LinkedState.getInstance());

        handler.sendToOrigin(DOCUMENT_START);
        handler.sendToOrigin(element); // send client's stream start to origin
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
        throw new IllegalStateException("Unexpected state:" + this);
    }
}
