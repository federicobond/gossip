package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class ValidatingCredentialsState extends
        XMPPHandlerState<OriginToClientXMPPStreamHandler> {
    private static final ValidatingCredentialsState INSTANCE = new ValidatingCredentialsState();

    protected static ValidatingCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ValidatingCredentialsState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() != AUTH_SUCCESS) {
            // disconnect client - no retries allowed
            handler.sendErrorToClient(BAD_FORMAT);
            return;
        }
        // buffer contents
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // buffer contents
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case STREAM_START:
            handler.sendToClient(element);
            handler.endHandling();
            break;
        case AUTH_SUCCESS:
            handler.setState(AuthenticatedState.getInstance());
            handler.sendToClient(element);
            handler.resetStream();
            handler.resumeTwin();
            break;
        default:
            throw new RuntimeException(); // will never happen
        }
    }
}
