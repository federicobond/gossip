package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private final XMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private HandlerState<OriginToClientXMPPStreamHandler> state = InitialState
            .getInstance();

    public OriginToClientXMPPStreamHandler(final XMPPConversation conversation,
            final OutputStream toClient, final OutputStream toOrigin)
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

    protected boolean isMuted(Message message) {
        // TODO: change this!
        return isCurrentUserMuted() || message.getSender().contains("mute");
    }

    protected boolean isCurrentUserMuted() {
        // TODO: change this!
        return conversation.getCredentials().getUsername().contains("mute");
    }

    String encodeCredentials() {
        return conversation.getCredentials().encode();
    }

    protected void sendToClient(PartialXMPPElement element) {
        // System.out.println("\n<O2C sending to client>");
        String currentContent = element.serializeCurrentContent();
        // System.out.println("Message:\n'"
        // + StringEscapeUtils.escapeJava(currentContent) + "' (string) "
        // + ArrayUtils.toString(currentContent.getBytes()));
        sendToClient(currentContent);
        // System.out.println("\nOutgoing buffer afterwards:");
        // ((ByteBufferOutputStream) toClient).printBuffer(false, true, true);
        // System.out.println("</O2C sending to client>\n");
    }

    protected void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected void sendToOrigin(String payload) {
        writeTo(toOrigin, payload);
    }

    @Override
    protected void resetStream() {
        super.resetStream();
    }

    void setState(final HandlerState<OriginToClientXMPPStreamHandler> state) {
        this.state = state;
    }
}
