package ar.edu.itba.it.gossip.util;

import static ar.edu.itba.it.gossip.util.CollectionUtils.asPair;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.tuple.Pair;

public abstract class XMLUtils {
    public static String DOCUMENT_START = "<?xml version=\"1.0\"?>";

    public static Pair<String, String> attr(String key, String value) {
        return asPair(key, value);
    }

    @SafeVarargs
    public static String attributes(Pair<String, String>... attributes) {
        String attributesSerialization = "";
        for (Pair<String, String> attribute : attributes) {
            attributesSerialization = concatKeyValue(attributesSerialization,
                    attribute.getKey(), attribute.getValue());
        }
        return attributesSerialization;
    }

    public static String serializeQName(QName qname) {
        String prefix = qname.getPrefix();
        if (prefix.isEmpty()) {
            return qname.getLocalPart();
        }
        return prefix + ":" + qname.getLocalPart();
    }

    public static String serializeNamespaces(Map<String, String> namespaces) {
        String serialization = "";
        for (Entry<String, String> entry : namespaces.entrySet()) {
            String localPart = entry.getKey();
            String namespace = localPart.isEmpty() ? "xmlns"
                    : ("xmlns:" + localPart);
            serialization = concatKeyValue(serialization, namespace,
                    entry.getValue());
        }
        return serialization;
    }

    public static String serializeAttributes(Map<String, String> attributes) {
        String serialization = "";
        for (Entry<String, String> entry : attributes.entrySet()) {
            serialization = concatKeyValue(serialization, entry.getKey(),
                    entry.getValue());
        }
        return serialization;
    }

    private static String serializeKeyValue(String key, String value) {
        return key + "=\"" + value + "\"";
    }

    public static String concatKeyValue(String baseString, String key,
            String value) {
        return baseString + " " + serializeKeyValue(key, value);
    }
}
