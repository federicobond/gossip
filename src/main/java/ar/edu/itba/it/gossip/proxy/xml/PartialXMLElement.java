package ar.edu.itba.it.gossip.proxy.xml;

import static ar.edu.itba.it.gossip.util.Validations.assumeState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class PartialXMLElement {
    private final List<Part> parts;

    public PartialXMLElement() {
        this.parts = new LinkedList<>();
    }

    public PartialXMLElement loadName(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeState(getNamePart() == null, "%s already has a name", this);

        parts.add(new NamePart(from.getLocalName())); // TODO: check!
        return this;
    }

    public PartialXMLElement loadAttributes(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumePartsExist(NamePart.class);
        assumeState(getAttributesPart() == null, "%s already has attributes",
                this);

        AttributesPart attributesPart = new AttributesPart();
        String localName, value;
        for (int i = 0; i < from.getAttributeCount(); i++) {
            localName = from.getAttributeLocalName(i); // TODO: check!
            value = from.getAttributeValue(i);
            attributesPart.attributes.put(localName, value);
        }

        return this;
    }

    public PartialXMLElement appendToBody(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);

        BodyPart bodyPart = new BodyPart(from.getText());
        parts.add(bodyPart);

        return this;
    }

    public PartialXMLElement addChild(PartialXMLElement child) {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);

        ChildPart childPart = new ChildPart(child);
        parts.add(childPart);

        return this;
    }

    public PartialXMLElement end() {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);

        parts.add(new EndPart(getName()));

        return this;
    }

    public String serializeCurrentContent() {
        String serialization = new String();

        for (Part part : parts) {
            if (!part.serialized) {
                serialization += part.serialize();
                if (!part.serialized) { // that is, if the part isn't completely
                                        // serialized yet
                    return serialization;
                }
            }
        }

        return serialization;
    }

    public String getName() {
        Optional<NamePart> namePartOpt = getNamePart();
        assumeState(namePartOpt.isPresent(), "Element's name is not set %s",
                this);
        return namePartOpt.get().name;
    }

    public Map<String, String> getAttributes() {
        Optional<AttributesPart> attributesPartOpt = getAttributesPart();
        assumeState(attributesPartOpt.isPresent(),
                "Element's attributes not set %s", this);
        return attributesPartOpt.get().attributes;
    }

    public String getBody() {
        Stream<BodyPart> bodyParts = getPartsOfClassAsStream(BodyPart.class, 3);
        return bodyParts.map(bodyPart -> bodyPart.text).collect(
                Collectors.joining());
    }

    public Iterable<PartialXMLElement> getChildren() {
        return getPartsOfClassAsStream(ChildPart.class).map(
                childPart -> childPart.child).collect(Collectors.toList());
    }

    public boolean areCurrentContentsFullySerialized() {
        return parts.get(parts.size() - 1).serialized;
    }

    @SafeVarargs
    private final void assumePartsExist(Class<? extends Part>... partClasses) {
        List<Class<? extends Part>> partClassesList = Arrays
                .asList(partClasses);

        Stream<Class<? extends Part>> matches = parts
                .stream()
                .filter(part -> partClassesList.stream().anyMatch(
                        partClass -> partClass.isInstance(part)))
                .map(part -> part.getClass());
        assumeState(matches.count() == partClasses.length,
                "Element expected parts %s to exist, but only %s exist",
                matches.collect(Collectors.toList()));
    }

    private void assumeNotEnded() {
        assumeState(!getEndPart().isPresent(), "Element already ended %s", this);
    }

    private Optional<NamePart> getNamePart() {
        return getPartByIndex(NamePart.class, 0);
    }

    private Optional<AttributesPart> getAttributesPart() {
        return getPartByIndex(AttributesPart.class, 1);
    }

    private Optional<EndPart> getEndPart() {
        Part lastPart = parts.get(parts.size() - 1);
        if (lastPart instanceof EndPart) {
            return Optional.of((EndPart) lastPart);
        }
        return Optional.empty();
    }

    private <P extends Part> Optional<P> getPartByIndex(Class<P> partClass,
            int i) {
        if (parts.size() >= i) {
            return Optional.of(partClass.cast(parts.get(i)));
        }
        return Optional.empty();
    }

    private <P extends Part> Stream<P> getPartsOfClassAsStream(
            Class<P> partClass) {
        return parts.stream().filter(part -> partClass.isInstance(part))
                .map(part -> partClass.cast(part));
    }

    private <P extends Part> Stream<P> getPartsOfClassAsStream(
            Class<P> partClass, int fromIdx) {
        return parts.subList(fromIdx, parts.size() - 1).stream()
                .filter(part -> partClass.isInstance(part))
                .map(part -> partClass.cast(part));
    }

    private abstract class Part {
        boolean serialized = false;

        String serialize() {
            String serialization = getSerialization();
            this.serialized = true;
            return serialization;
        }

        abstract String getSerialization();
    }

    private class NamePart extends Part {
        final String name;

        NamePart(final String name) {
            this.name = name;
        }

        @Override
        String getSerialization() {
            return "<" + name;
        }
    }

    private class AttributesPart extends Part {
        final Map<String, String> attributes = new HashMap<>();

        @Override
        String getSerialization() {
            String serialization = new String();
            for (Entry<String, String> entry : attributes.entrySet()) {
                serialization += " " + entry.getKey() + "=" + entry.getValue();
            }
            return serialization + ">";
        }
    }

    private class BodyPart extends Part {
        String text;

        BodyPart(final String text) {
            this.text = text;
        }

        @Override
        String serialize() {
            // TODO Auto-generated method stub
            return super.serialize();
        }

        @Override
        String getSerialization() {
            return text;
        }
    }

    private class ChildPart extends Part {
        final PartialXMLElement child;

        ChildPart(final PartialXMLElement child) {
            this.child = child;
        }

        @Override
        String serialize() {
            String serialization = getSerialization();
            serialized = child.areCurrentContentsFullySerialized();
            return serialization;
        }

        @Override
        String getSerialization() {
            return child.serializeCurrentContent();
        }
    }

    private class EndPart extends Part {
        final String name;

        public EndPart(final String name) {
            this.name = name;
        }

        @Override
        String getSerialization() {
            return "</" + name + ">";
        }
    }
}
