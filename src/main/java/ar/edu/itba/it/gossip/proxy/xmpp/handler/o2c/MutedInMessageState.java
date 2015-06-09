package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.MutableChatState;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class MutedInMessageState extends XMPPHandlerState<OriginToClientXMPPStreamHandler> {
    private static final MutedInMessageState INSTANCE = new MutedInMessageState();
    private final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    protected static MutedInMessageState getInstance() {
        return INSTANCE;
    }

    protected MutedInMessageState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case BODY:
        case SUBJECT:
            element.consumeCurrentContent();
            break;
        case COMPOSING:
        case PAUSED:
            ((MutableChatState) element).mute();
            // fall through
        default:
            handler.sendToClient(element);
        }
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case SUBJECT:
        case BODY:
        case MESSAGE:// you are muted, don't try and send text in message
                     // tags =)
            element.consumeCurrentContent();
            break;
        default:
            handler.sendToClient(element);
        }
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case SUBJECT:
        case BODY:
            element.consumeCurrentContent();
            break;
        case MESSAGE:
            if (!handler.isClientMuted()) {
                // TODO: this assumes that messages cannot be embedded into
                // other messages or anything like that! If that were the
                // case, this *will* fail
                handler.setState(LinkedState.getInstance());
            } else {
                handler.setState(MutedOutsideMessageState.getInstance());
            }
            proxyConfig.countMessageMutedIn();
            // fall through
        default:
            handler.sendToClient(element);
        }
    }
}
