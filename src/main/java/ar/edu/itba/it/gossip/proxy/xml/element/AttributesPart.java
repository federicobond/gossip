package ar.edu.itba.it.gossip.proxy.xml.element;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import static java.util.Collections.*;
import ar.edu.itba.it.gossip.util.XMLUtils;

class AttributesPart extends Part {
    private final Map<String, String> attributes = new HashMap<>();

    AttributesPart(AsyncXMLStreamReader<?> from) {
        for (int i = 0; i < from.getAttributeCount(); i++) {
            String localName = from.getAttributeLocalName(i); // TODO: check!
            String value = from.getAttributeValue(i);
            attributes.put(localName, value);
        }
    }

    Map<String, String> getAttributes() {
        return unmodifiableMap(attributes);
    }

    @Override
    String getSerialization() {
        return XMLUtils.serializeAttributes(attributes) + ">";
    }
}
