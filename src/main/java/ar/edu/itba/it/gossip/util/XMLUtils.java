package ar.edu.itba.it.gossip.util;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

public abstract class XMLUtils {
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
            serialization += " "
                    + serializeKeyValue(namespace, entry.getValue());
        }
        return serialization;
    }

    public static String serializeAttributes(Map<String, String> attributes) {
        String serialization = "";
        for (Entry<String, String> entry : attributes.entrySet()) {
            serialization += " "
                    + serializeKeyValue(entry.getKey(), entry.getValue());
        }
        return serialization;
    }

    private static String serializeKeyValue(String key, String value) {
        return key + "=\"" + value + "\"";
    }
}
