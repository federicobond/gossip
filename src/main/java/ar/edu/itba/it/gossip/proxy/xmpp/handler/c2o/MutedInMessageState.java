package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.message;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.MutableChatState;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class MutedInMessageState extends XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
    private static final MutedInMessageState INSTANCE = new MutedInMessageState();
    private final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    protected static MutedInMessageState getInstance() {
        return INSTANCE;
    }

    protected MutedInMessageState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case BODY:
        case SUBJECT:
            if (!handler.isClientNotifiedOfMute()) {
                Message message = (Message) element.getParent().get();
                sendMutedNotificationToClient(handler, message);
                handler.setClientNotifiedOfMute(true);
            }
            element.consumeCurrentContent();
            break;
        case COMPOSING:
        case PAUSED:
            ((MutableChatState) element).mute();
            // fall through
        default:
            handler.sendToOrigin(element);
            break;
        }
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case SUBJECT:
        case BODY:
        case MESSAGE:// you are muted, don't try and send text in message
                     // tags =)
            element.consumeCurrentContent();
            break;
        default:
            handler.sendToOrigin(element);
        }
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case SUBJECT:
        case BODY:
            element.consumeCurrentContent();
            break;
        case MESSAGE:
            Message message = (Message) element;
            if (!handler.isClientMuted()) {
                // TODO: this assumes that messages cannot be embedded into
                // other messages or anything like that! If that were the
                // case, this *will* fail
                handler.setState(LinkedState.getInstance());
                if (handler.isClientCauseOfMute()) {
                    sendUnmutedNotificationToClient(handler, message);
                }
                proxyConfig.countMessageMutedOut();
            } else {
                handler.setState(MutedOutsideMessageState.getInstance());
            }
            // fall through
        default:
            handler.sendToOrigin(element);
        }
    }

    private void sendMutedNotificationToClient(
            ClientToOriginXMPPStreamHandler handler, Message message) {
        final String msg;
        if (handler.isClientCauseOfMute()) {
            msg = "You have been muted, you will not be able to talk to other users";
        } else {
            msg = message.getReceiver()
                    + " has been muted, you will not be able to talk to them";
        }
        sendNotificationToClient(handler, message.getReceiver(), msg);
    }

    private void sendUnmutedNotificationToClient(
            ClientToOriginXMPPStreamHandler handler, Message message) {
        sendNotificationToClient(handler, message.getReceiver(),
                "You are free to talk again");
    }

    private void sendNotificationToClient(
            ClientToOriginXMPPStreamHandler handler, String receiver,
            String text) {
        handler.sendToClient(message(receiver, handler.getCurrentUser(), text));
    }
}
