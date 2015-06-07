package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

class LinkedState extends HandlerState {
    private static final LinkedState INSTANCE = new LinkedState();

    protected static LinkedState getInstance() {
        return INSTANCE;
    }

    protected LinkedState() {
    }

    @Override
    protected void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == MESSAGE) {
            Message message = (Message) element;
            if (handler.isMuted(message)) {
                handler.setClientNotifiedOfMute(false);
                handler.setClientCauseOfMute(handler.isCurrentUserMuted());
                handler.setState(MutedInMessageState.getInstance());
            } else {
                // TODO: check config to see if leet conversion should be
                // enabled
                message.enableLeetConversion();
            }
        }
        sendToOrigin(handler, element);
    }

    @Override
    protected void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // this is here just in case leet conversion was enabled by the
        // admin after the message's start tag
        if (element.getType() == MESSAGE) {
            ((Message) element).enableLeetConversion();
        }
        sendToOrigin(handler, element);
    }

    @Override
    protected void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        sendToOrigin(handler, element);
    }
}
