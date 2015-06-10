package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.STREAM_ERROR;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class ExpectAuthFeaturesState extends
        XMPPHandlerState<OriginToClientXMPPStreamHandler> {
    private static final ExpectAuthFeaturesState INSTANCE = new ExpectAuthFeaturesState();

    protected static ExpectAuthFeaturesState getInstance() {
        return INSTANCE;
    }

    protected ExpectAuthFeaturesState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == STREAM_ERROR) {
            handler.sendErrorToClient(BAD_FORMAT);
        }
        element.consumeCurrentContent();
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        element.consumeCurrentContent();
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case STREAM_START:
            handler.sendErrorToClient(BAD_FORMAT);
            break;
        case AUTH_FEATURES:
            sendAuthDataToOrigin(handler);
            handler.setState(ValidatingCredentialsState.getInstance());
            // fall through
        default:
            element.consumeCurrentContent();
        }
    }

    protected void sendAuthDataToOrigin(OriginToClientXMPPStreamHandler handler) {
        handler.sendToOrigin(auth("PLAIN", handler.encodeCredentials()));
    }
}
