package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.XMLUtils.serializeAttributes;
import static ar.edu.itba.it.gossip.util.XMLUtils.serializeNamespaces;
import static ar.edu.itba.it.gossip.util.XMLUtils.serializeQName;
import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.aalto.AsyncXMLStreamReader;

class AttributesPart extends Part {
    private final Map<String, String> namespaces = new LinkedHashMap<>();
    private final Map<String, String> attributes = new LinkedHashMap<>();

    AttributesPart(AsyncXMLStreamReader<?> from) {
        for (int i = 0; i < from.getNamespaceCount(); i++) {
            String prefix = from.getNamespacePrefix(i);
            String uri = from.getNamespaceURI(i);
            namespaces.put(prefix, uri);
        }

        for (int i = 0; i < from.getAttributeCount(); i++) {
            String name = serializeQName(from.getAttributeName(i));
            String value = from.getAttributeValue(i);
            attributes.put(name, value);
        }
    }

    Map<String, String> getAttributes() {
        return unmodifiableMap(attributes);
    }

    Map<String, String> getNamespaces() {
        return unmodifiableMap(namespaces);
    }

    @Override
    String getSerialization() {
        return serializeNamespaces(namespaces)
                + serializeAttributes(attributes) + ">";
    }
}
