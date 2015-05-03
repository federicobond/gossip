package ar.edu.itba.it.gossip.proxy.xmpp.event;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class AuthStanza extends XMPPEvent {
    private final String mechanism;
    private final String username;
    private final String password;

    public AuthStanza(Map<String, String> attributes, String body) {
        this.mechanism = attributes.get("mechanism");

        // TODO: this is not tolerant to auth without initial \0
        body = body.replaceAll("\n", "");
        String[] parts = new String(Base64.getDecoder().decode(body),
                StandardCharsets.UTF_8).split("\0");

        this.username = parts[1];
        this.password = parts[2];
    }

    @Override
    public Type getType() {
        return Type.AUTH;
    }

    public String getMechanism() {
        return mechanism;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean mechanismMatches(String mechanism) {
        return this.mechanism.equals(mechanism);
    }
}
