package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.util.XMPPUtils.auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class ExpectAuthFeaturesState extends
        HandlerState<OriginToClientXMPPStreamHandler> {
    private static final ExpectAuthFeaturesState INSTANCE = new ExpectAuthFeaturesState();

    protected static ExpectAuthFeaturesState getInstance() {
        return INSTANCE;
    }

    protected ExpectAuthFeaturesState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // TODO: check! should NEVER happen!

        // ^ well, it actually does, here I am... TODO: check for potential
        // floods!
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
        switch (element.getType()) {
        case AUTH_REGISTER:
        case AUTH_MECHANISMS:
        case AUTH_MECHANISM:
            break;
        case AUTH_FEATURES:
            sendAuthDataToOrigin(handler);
            handler.setState(ValidatingCredentialsState.getInstance());
            break;
        default:
            throw new IllegalStateException("Unexpected event type: "
                    + element.getType());
        }
    }

    protected void sendAuthDataToOrigin(OriginToClientXMPPStreamHandler handler) {
        handler.sendToOrigin(auth("PLAIN", handler.encodeCredentials()));
    }
}
