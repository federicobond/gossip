package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;

public class ClientToOriginXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toOrigin;
    private final OutputStream toClient;

    private HandlerState<ClientToOriginXMPPStreamHandler> state = InitialState
            .getInstance();
    private boolean clientNotifiedOfMute;
    private boolean clientCauseOfMute;

    public ClientToOriginXMPPStreamHandler(final XMPPConversation conversation,
            final TCPStream clientToOrigin, final OutputStream toClient)
            throws XMLStreamException {
        super(clientToOrigin);
        this.conversation = conversation;
        this.toOrigin = clientToOrigin.getOutputStream();
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

    protected void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected void sendToOrigin(PartialXMPPElement element) {
        // System.out.println("\n<C2O sending to origin>");
        String currentContent = element.serializeCurrentContent();
        // System.out.println("Message:\n'"
        // + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
        // + ArrayUtils.toString(currentContent.getBytes()));
        sendToOrigin(currentContent);
        // System.out.println("\nOutgoing buffer afterwards:");
        // ((ByteBufferOutputStream) toOrigin).printBuffer(false, true, true);
        // System.out.println("</C2O sending to origin>\n");
    }

    String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    boolean isMuted(Message message) {
        // TODO: change this!
        return isCurrentUserMuted() || message.getReceiver().contains("mute");
    }

    boolean isCurrentUserMuted() {
        // TODO: change this!
        return getCurrentUser().contains("mute");
    }

    void setState(final HandlerState<ClientToOriginXMPPStreamHandler> state) {
        this.state = state;
    }

    void setCredentials(Credentials credentials) {
        conversation.setCredentials(credentials);
    }

    @Override
    protected DeferredConnector getConnector() { // just for visibility
        return super.getConnector();
    }

    @Override
    protected void resetStream() { // just for visibility
        super.resetStream();
    }

    @Override
    protected void waitForTwin() { // just for visibility
        super.waitForTwin();
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
