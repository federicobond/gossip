package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xmpp.XMPPLeetTransformation;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class Message extends PartialXMPPElement {
    private static final String RECEIVER_ATTRIBUTE = "to";
    private static final XMPPLeetTransformation LEET_TRANSFORMATION = new XMPPLeetTransformation();

    public Message(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    public void enableLeetConversion() {
        if (!isBodyBeingTransformed()) {
            setBodyTransformation(LEET_TRANSFORMATION);
        }
    }

    public String getReceiver() {
        return getAttributes().get(RECEIVER_ATTRIBUTE);
    }
}
