package ar.edu.itba.it.gossip.proxy.xmpp.element;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import static ar.edu.itba.it.gossip.util.ValidationUtils.require;
import static ar.edu.itba.it.gossip.util.XMLUtils.serializeQName;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.util.PartiallySerializable;

public class PartialXMPPElement extends PartialXMLElement implements
        PartiallySerializable {
    public static PartialXMPPElement from(AsyncXMLStreamReader<?> reader) {
        String name = serializeQName(reader.getName());

        switch (Type.of(name)) {
        case AUTH_CHOICE:
            return new Auth(reader);
        case MESSAGE:
            return new Message(reader);
        default:
            return new PartialXMPPElement(reader);
        }
    }

    private final Type type;

    protected PartialXMPPElement(AsyncXMLStreamReader<?> reader) {
        loadName(reader);
        loadAttributes(reader);

        this.type = Type.of(getName());
    }

    public Type getType() {
        return type;
    }

    @Override
    public Optional<PartialXMPPElement> getParent() {
        // NOTE: mapping with getClass()::cast wouldn't be appropriate here
        // since parents could be instances of *any* subtype of
        // PartialXMPPElement
        return super.getParent().map(PartialXMPPElement.class::cast);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    public enum Type {
        STREAM_START("stream:stream"), AUTH_CHOICE("auth"), AUTH_FEATURES(
                "stream:features"), AUTH_REGISTER("register"), AUTH_MECHANISMS(
                "mechanisms"), AUTH_MECHANISM("mechanism"), AUTH_SUCCESS(
                "success"), AUTH_FAILURE("failure"), MESSAGE("message"), OTHER;

        private static final Map<String, Type> typesByName;

        static {
            // NOTE: This is here solely because of the really limited access to
            // static fields Java enums have

            Map<String, Type> typesMap = new HashMap<>();
            for (Type type : values()) {
                if (type.name.isPresent()) {
                    assumeState(!typesMap.containsKey(type.name.get()),
                            "Name %s is repeated");
                    typesMap.put(type.name.get(), type);
                }
            }
            typesByName = unmodifiableMap(typesMap);
        }

        private final Optional<String> name;

        Type() {
            this.name = Optional.empty();
        }

        Type(String name) {
            this.name = Optional.of(name);
        }

        static Type of(String name) {
            require(name != null);
            return typesByName.getOrDefault(name, OTHER);
        }
    }
}
