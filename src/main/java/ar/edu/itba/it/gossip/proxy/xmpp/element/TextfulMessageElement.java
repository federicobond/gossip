package ar.edu.itba.it.gossip.proxy.xmpp.element;

import ar.edu.itba.it.gossip.proxy.xml.element.BodyPart;
import ar.edu.itba.it.gossip.proxy.xml.element.Part;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPLeetTransformation;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class TextfulMessageElement extends PartialXMPPElement {
    private static final XMPPLeetTransformation LEET_TRANSFORMATION = new XMPPLeetTransformation();

    private boolean transformBody = false;

    protected TextfulMessageElement(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    void enableLeetConversion() {
        transformBody = true;
    }

    @Override
    protected String serialize(Part part) {
        final String serialization = super.serialize(part);
        if (part instanceof BodyPart && transformBody) {
            return LEET_TRANSFORMATION.apply(serialization);
        }
        return serialization;
    }

    @Override
    public String serializeCurrentContent() {
        return super.serializeCurrentContent();
    }
}
