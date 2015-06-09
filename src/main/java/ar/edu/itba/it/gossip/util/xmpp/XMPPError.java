package ar.edu.itba.it.gossip.util.xmpp;

public enum XMPPError {
    BAD_FORMAT("bad-format"), MALFORMED_REQUEST("malformed-request");

    private final String tagName;

    private XMPPError(final String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }
}
