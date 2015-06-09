package ar.edu.itba.it.gossip.proxy.xmpp.handler.o2c;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStream;
import ar.edu.itba.it.gossip.proxy.xmpp.ProxiedXMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Message;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.HandlerState;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPStreamHandler;

public class OriginToClientXMPPStreamHandler extends XMPPStreamHandler {
    private static final ProxyConfig proxyConfig = ProxyConfig.getInstance();

    private final ProxiedXMPPConversation conversation;
    private final OutputStream toClient;
    private final OutputStream toOrigin;

    private HandlerState<OriginToClientXMPPStreamHandler> state = InitialState
            .getInstance();

    public OriginToClientXMPPStreamHandler(
            final ProxiedXMPPConversation conversation,
            final TCPStream originToClient, final OutputStream toOrigin)
            throws XMLStreamException {
        super(originToClient);
        this.conversation = conversation;
        this.toOrigin = toOrigin;
        this.toClient = originToClient.getOutputStream();
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

    String getCurrentUser() {
        return conversation.getCredentials().getUsername();
    }

    boolean isMuted(Message message) {
        String from = message.getSender();
        if (from == null) { // administrative message from own server
            return isClientMuted();
        }
        return isJIDMuted(from) || isClientMuted();
    }

    boolean isClientMuted() {
        return isJIDMuted(proxyConfig.getJID(getCurrentUser()));
    }

    private boolean isJIDMuted(String jid) {
        return proxyConfig.isJIDSilenced(jid);
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
    protected void resetStream() { // just for visibility
        super.resetStream();
    }

    @Override
    protected void resumeTwin() { // just for visibility
        super.resumeTwin();
    }

    void setState(final HandlerState<OriginToClientXMPPStreamHandler> state) {
        this.state = state;
    }

}
