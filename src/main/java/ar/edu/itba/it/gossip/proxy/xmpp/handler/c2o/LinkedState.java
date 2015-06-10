package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.BAD_FORMAT;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class LinkedState extends XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
    private static final LinkedState INSTANCE = new LinkedState();
    private static final ProxyConfig proxyConfig = ProxyConfig.getInstance();

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
                handler.setClientCauseOfMute(handler.isClientMuted());
                handler.setState(MutedInMessageState.getInstance());
            } else {
                if (proxyConfig.convertLeet()) {
                    message.enableLeetConversion();
                }
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
            if (proxyConfig.convertLeet()) {
                ((Message) element).enableLeetConversion();
            }
        }
        handler.sendToOrigin(element);
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        switch (element.getType()) {
        case STREAM_START:
            handler.sendErrorToClient(BAD_FORMAT);
            break;
        case MESSAGE:
            proxyConfig.countSentMessage();
            // fall through
        default:
            handler.sendToOrigin(element);
        }
    }
}
