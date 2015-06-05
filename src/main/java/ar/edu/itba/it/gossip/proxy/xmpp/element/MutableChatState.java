package ar.edu.itba.it.gossip.proxy.xmpp.element;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class MutableChatState extends PartialXMPPElement {
    private static final String NAME_WHEN_MUTED = "active";

    private boolean muted = false;

    protected MutableChatState(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    public void mute() {
        assumeState(!muted, "%s is already muted", this);
        muted = true;
        modifyName(NAME_WHEN_MUTED);
    }
}
