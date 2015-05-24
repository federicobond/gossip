package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPLeetTransformation;

public class Message extends PartialXMPPElement {
    private static final XMPPLeetTransformation LEET_TRANSFORMATION = new XMPPLeetTransformation();

    Message(PartialXMLElement element) {
        super(element);
    }

    public void enableLeetConversion() {
        PartialXMLElement xmlElement = getXML();
        if (!xmlElement.isBodyBeingTransformed()) {
            getXML().setBodyTransformation(LEET_TRANSFORMATION);
        }
    }

    @Override
    public String serializeCurrentContent() {
        return super.serializeCurrentContent();
    }
}
