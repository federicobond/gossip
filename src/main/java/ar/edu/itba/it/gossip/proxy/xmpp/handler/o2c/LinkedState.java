package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.MESSAGE;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;

class LinkedState extends HandlerState<OriginToClientXMPPStreamHandler> {
    private static final LinkedState INSTANCE = new LinkedState();
    private final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    protected static LinkedState getInstance() {
        return INSTANCE;
    }

    protected LinkedState() {
    }

    @Override
    public void handleStart(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() == MESSAGE) {
            Message message = (Message) element;
            if (handler.isMuted(message)) {
                handler.setState(MutedInMessageState.getInstance());
            } else {
                // TODO: if you want to convert messages from outside origin
                // to leet, this is the place!
            }
        }
        handler.sendToClient(element);
    }

    @Override
    public void handleBody(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // TODO: check for leet case!
        handler.sendToClient(element);
    }

    @Override
    public void handleEnd(OriginToClientXMPPStreamHandler handler,
            PartialXMPPElement element) {
        handler.sendToClient(element);
        if (element.getType() == MESSAGE) {
            proxyConfig.countReceivedMessage();
        }
    }
}
