package ar.edu.itba.it.gossip.util;

import java.util.Map;
import java.util.Map.Entry;

public abstract class XMLUtils {
    public static String serializeAttributes(Map<String, String> attributes) {
        String serialization = "";
        for (Entry<String, String> entry : attributes.entrySet()) {
            serialization += " " + entry.getKey() + "=" + entry.getValue();
        }
        return serialization;
    }
}
