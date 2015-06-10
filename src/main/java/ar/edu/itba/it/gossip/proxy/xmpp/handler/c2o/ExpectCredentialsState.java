package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPError.*;
import static ar.edu.itba.it.gossip.util.xmpp.XMPPUtils.streamOpen;

import java.net.InetSocketAddress;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.handler.XMPPHandlerState;

class ExpectCredentialsState extends
        XMPPHandlerState<ClientToOriginXMPPStreamHandler> {
    private static final ExpectCredentialsState INSTANCE = new ExpectCredentialsState();

    protected static ExpectCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ExpectCredentialsState() {
    }

    @Override
    public void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // just buffer auth choice's start tag: the rest, discard
        if (element.getType() != AUTH_CHOICE) {
            element.consumeCurrentContent();
        }
    }

    @Override
    public void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // just buffer auth choice's contents: the rest, discard
        if (element.getType() != AUTH_CHOICE) {
            element.consumeCurrentContent();
        }
    }

    @Override
    public void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        if (element.getType() != AUTH_CHOICE) {
            handler.sendErrorToClient(BAD_FORMAT);
            return;
        }

        final Credentials credentials;
        try {
            credentials = ((Auth) element).getCredentials();
        } catch (IllegalArgumentException exc) {
            handler.sendErrorToClient(MALFORMED_REQUEST);
            return;
        }
        System.out.println(credentials.getUsername()
                + " is trying to log in with password: "
                + credentials.getPassword());
        handler.setCredentials(credentials);

        connectToOrigin(handler);
        sendStreamOpenToOrigin(handler);
        handler.resetStream();

        handler.setState(ValidatingCredentialsState.getInstance());
        handler.waitForTwin();
    }

    private void connectToOrigin(ClientToOriginXMPPStreamHandler handler) {
        String currentUser = handler.getCurrentUser();
        InetSocketAddress address = getProxyConfig().getOriginAddress(
                currentUser);

        handler.getConnector().connectToOrigin(address);
    }

    private void sendStreamOpenToOrigin(ClientToOriginXMPPStreamHandler handler) {
        String currentUser = getProxyConfig().getJID(handler.getCurrentUser());
        String originName = getProxyConfig().getXMPPServerName();

        handler.sendToOrigin(DOCUMENT_START
                + streamOpen(currentUser, originName));
    }
}
