package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class Message extends PartialXMPPElement {
    private static final String RECEIVER_ATTRIBUTE = "to";

    private String receiver;
    private boolean transformBody = false;

    public Message(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    @Override
    public PartialXMLElement loadAttributes(AsyncXMLStreamReader<?> from) {
        super.loadAttributes(from);
        this.receiver = getAttributes().get(RECEIVER_ATTRIBUTE);
        return this;
    }

    @Override
    public PartialXMLElement addChild(PartialXMLElement child) {
        super.addChild(child);
        if (child instanceof TextfulMessageElement && transformBody) {
            ((TextfulMessageElement) child).enableLeetConversion();
        }
        return this;
    }

    public void enableLeetConversion() {
        this.transformBody = true;
    }

    public String getReceiver() {
        return receiver;
    }
}
