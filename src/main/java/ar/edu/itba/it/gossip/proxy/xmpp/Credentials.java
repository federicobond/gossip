package ar.edu.itba.it.gossip.proxy.xmpp;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Credentials {
    private static final String SEPARATOR = "\0";

    private final String username;
    private final String password;

    public static Credentials decode(String str) {
        // TODO: this is not tolerant to auth without initial \0
        String sanitizedStr = str.replaceAll("\n", "");
        String[] parts = new String(Base64.getDecoder().decode(sanitizedStr),
                StandardCharsets.UTF_8).split(SEPARATOR);

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid credentials string: "
                    + str);
        }
        return new Credentials(parts[1], parts[2]);
    }

    public Credentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String encode() {
        String credentials = SEPARATOR + username + SEPARATOR + password;
        return new String(Base64.getEncoder().encode(credentials.getBytes()),
                StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
