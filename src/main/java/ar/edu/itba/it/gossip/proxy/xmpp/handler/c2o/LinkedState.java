package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class LinkedState extends HandlerState<ClientToOriginXMPPStreamHandler> {
    private static final LinkedState INSTANCE = new LinkedState();

    protected static LinkedState getInstance() {
        return INSTANCE;
    }

    protected LinkedState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
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
        handler.sendToOrigin(element);
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // this is here just in case leet conversion was enabled by the
        // admin after the message's start tag
        if (element.getType() == MESSAGE) {
            ((Message) element).enableLeetConversion();
        }
        handler.sendToOrigin(element);
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToOrigin(element);
    }
}