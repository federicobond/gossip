package ar.edu.itba.it.gossip.util;

import static ar.edu.itba.it.gossip.util.XMLUtils.attr;
import static ar.edu.itba.it.gossip.util.XMLUtils.*;
import static ar.edu.itba.it.gossip.util.XMLUtils.concatKeyValue;

public abstract class XMPPUtils {
    private static String STREAM_OPEN_TAG_MANDATORY_PREFIX = "<stream:stream"
            + attributes(
                    attr("xmlns:stream", "http://etherx.jabber.org/streams"),
                    attr("version", "1.0"), attr("xmlns", "jabber:client"),
                    attr("xml:lang", "en"),
                    attr("xmlns:xml", "http://www.w3.org/XML/1998/namespace"));

    private static String REGISTER_FEATURE = "<register"
            + attributes(attr("xmlns", "http://jabber.org/features/iq-register"))
            + "/>";
    private static String MECHANISMS_OPEN = "<mechanisms"
            + attributes(attr("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl"))
            + ">";
    private static String STREAM_FEATURES_TAG_PREFIX = "<stream:features>"
            + REGISTER_FEATURE + MECHANISMS_OPEN;

    private static String MESSAGE_PREFIX = "<message"
            + attributes(attr("type", "chat"));

    public static String streamOpen() {
        return STREAM_OPEN_TAG_MANDATORY_PREFIX + ">";
    }

    public static String streamOpen(String from, String to) {
        String suffix = "";
        if (from != null) {
            suffix = concatKeyValue(suffix, "from", from);
        }
        if (to != null) {
            suffix = concatKeyValue(suffix, "to", to);
        }
        return STREAM_OPEN_TAG_MANDATORY_PREFIX + suffix + ">";
    }

    public static String streamFeatures(String... mechanisms) {
        String suffix = "";
        for (String mechanism : mechanisms) {
            suffix += "<mechanism>" + mechanism + "</mechanism>";
        }
        return STREAM_FEATURES_TAG_PREFIX + suffix + "</mechanisms>"
                + "</stream:features>";
    }

    public static String message(String from, String to, String bodyText) {
        return MESSAGE_PREFIX + attributes(attr("from", from), attr("to", to))
                + ">" + "<body>" + bodyText + "</body>" + "</message>";
    }

    public static String streamError() {
        return DOCUMENT_START
                + "<stream:stream"
                + attributes(
                        attr("id", ""),
                        attr("xmlns:stream", "http://etherx.jabber.org/streams"),
                        attr("version", "1.0"), attr("xmlns", "jabber:client"))
                + "<stream:error>"
                + "<invalid-namespace"
                + attributes(attr("xmlns",
                        "urn:ietf:params:xml:ns:xmpp-streams")) + "/>"
                + "</stream:error>" + "</stream:stream>";
    }
}
