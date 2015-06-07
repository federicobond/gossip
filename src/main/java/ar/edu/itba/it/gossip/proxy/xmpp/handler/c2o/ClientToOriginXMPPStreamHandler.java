package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toOrigin;
    private final OutputStream toClient;

    private HandlerState state = InitialState.getInstance();
    private boolean clientNotifiedOfMute;
    private boolean clientCauseOfMute;

    public ClientToOriginXMPPStreamHandler(final XMPPConversation conversation,
            final OutputStream toOrigin, final OutputStream toClient)
            throws XMLStreamException {
        this.conversation = conversation;
        this.toOrigin = toOrigin;
        this.toClient = toClient;
    }

    @Override
    public void handleStart(PartialXMPPElement element) {
        state.handleStart(this, element);
    }

    @Override
    public void handleBody(PartialXMPPElement element) {
        state.handleBody(this, element);
    }

    @Override
    public void handleEnd(PartialXMPPElement element) {
        state.handleEnd(this, element);
    }

    protected void sendToOrigin(String message) {
        writeTo(toOrigin, message);
    }

    String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    void sendToClient(String message) {
        writeTo(toClient, message);
    }

    boolean isMuted(Message message) {
        // TODO: change this!
        return isCurrentUserMuted() || message.getReceiver().contains("mute");
    }

    boolean isCurrentUserMuted() {
        // TODO: change this!
        return getCurrentUser().contains("mute");
    }

    void setState(final HandlerState state) {
        this.state = state;
    }

    void setCredentials(Credentials credentials) {
        conversation.setCredentials(credentials);
    }

    @Override
    protected DeferredConnector getConnector() {
        return super.getConnector();
    }

    @Override
    protected void resetStream() {
        super.resetStream();
    }

    boolean isClientNotifiedOfMute() {
        return clientNotifiedOfMute;
    }

    void setClientNotifiedOfMute(final boolean value) {
        this.clientNotifiedOfMute = value;
    }

    boolean isClientCauseOfMute() {
        return clientCauseOfMute;
    }

    void setClientCauseOfMute(final boolean value) {
        this.clientCauseOfMute = value;
    }
}
