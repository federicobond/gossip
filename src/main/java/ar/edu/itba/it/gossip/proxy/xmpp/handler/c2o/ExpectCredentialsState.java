package ar.edu.itba.it.gossip.proxy.xmpp.handler.c2o;

import static ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.util.XMLUtils.DOCUMENT_START;
import static ar.edu.itba.it.gossip.util.XMPPUtils.streamOpen;

import java.net.InetSocketAddress;

import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;

class ExpectCredentialsState extends HandlerState {
    private static final ExpectCredentialsState INSTANCE = new ExpectCredentialsState();

    protected static ExpectCredentialsState getInstance() {
        return INSTANCE;
    }

    protected ExpectCredentialsState() {
    }

    @Override
    protected void handleStart(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // TODO: check! should NEVER happen!
    }

    @Override
    protected void handleBody(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        // do nothing, just buffer element's contents
        // TODO: check for potential floods!
    }

    @Override
    protected void handleEnd(ClientToOriginXMPPStreamHandler handler,
            PartialXMPPElement element) {
        assumeType(element, AUTH_CHOICE);
        Credentials credentials = ((Auth) element).getCredentials();
        System.out.println(credentials.getUsername()
                + " is trying to log in with password: "
                + credentials.getPassword());

        handler.setCredentials(credentials);

        connectToOrigin(handler);
        sendStreamOpenToOrigin(handler);
        handler.resetStream();

        handler.setState(ValidatingCredentialsState.getInstance());
    }

    private void connectToOrigin(ClientToOriginXMPPStreamHandler handler) {
        String currentUser = handler.getCurrentUser();
        InetSocketAddress address = getProxyConfig().getOriginAddress(
                currentUser);

        handler.getConnector().connectToOrigin(address);
    }

    private void sendStreamOpenToOrigin(ClientToOriginXMPPStreamHandler handler) {
        String currentUser = handler.getCurrentUser();
        String originName = getProxyConfig().getOriginName(currentUser);

        sendToOrigin(handler,
                DOCUMENT_START + streamOpen(currentUser, originName));
    }
}
