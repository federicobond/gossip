package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_FAILURE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class ValidatingCredentialsState extends
        HandlerState<OriginToClientXMPPStreamHandler> {
    private static final ValidatingCredentialsState INSTANCE = new ValidatingCredentialsState();

    protected static ValidatingCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ValidatingCredentialsState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == AUTH_FAILURE) {
            handler.setState(AuthFailedState.getInstance());
            handler.sendToClient(element);
        }
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
        case AUTH_SUCCESS:
            handler.setState(AuthenticatedState.getInstance());
            handler.sendToClient(element);
            handler.resetStream();
            break;
        case AUTH_FAILURE:// TODO
            handler.sendToClient(element);
            break;
        default:
            throw new IllegalStateException("Unexpected event type: "
                    + element.getType());
        }
    }
}
