package ar.edu.itba.it.gossip.proxy.xmpp.event;

public class AuthFeaturesEnd extends XMPPEvent {
    @Override
    public Type getType() {
        return Type.AUTH_FEATURES_END;
    }
}
