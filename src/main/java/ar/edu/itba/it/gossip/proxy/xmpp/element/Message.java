package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPLeetTransformation;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class Message extends PartialXMPPElement {
    private static final String RECEIVER_ATTRIBUTE = "to";
    private static final XMPPLeetTransformation LEET_TRANSFORMATION = new XMPPLeetTransformation();

    private String receiver;

    public Message(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    @Override
    public PartialXMLElement loadAttributes(AsyncXMLStreamReader<?> from) {
        super.loadAttributes(from);
        this.receiver = getAttributes().get(RECEIVER_ATTRIBUTE);
        return this;
    }

    public void enableLeetConversion() {
        if (!isBodyBeingTransformed()) {
            setBodyTransformation(LEET_TRANSFORMATION);
        }
    }

    public String getReceiver() {
        return receiver;
    }
}
