package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.CollectionUtils.last;
import static ar.edu.itba.it.gossip.util.PredicateUtils.isInstanceOfAny;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import static ar.edu.itba.it.gossip.util.ValidationUtils.require;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class PartialXMLElement {
    private Optional<PartialXMLElement> parent;
    private final List<Part> parts;

    public PartialXMLElement() {
        this.parts = new LinkedList<>();
        this.parent = Optional.empty();
    }

    public PartialXMLElement(final PartialXMLElement parent) {
        this();
        parent.addChild(this);
    }

    public PartialXMLElement loadName(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeState(!getNamePart().isPresent(), "%s already has a name", this);

        parts.add(new NamePart(from));
        return this;
    }

    public PartialXMLElement loadAttributes(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumePartsExist(NamePart.class);
        assumeState(!getAttributesPart().isPresent(),
                "%s already has attributes", this);

        parts.add(new AttributesPart(from));
        return this;
    }

    public PartialXMLElement appendToBody(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);

        parts.add(new BodyPart(from));
        return this;
    }

    public PartialXMLElement addChild(PartialXMLElement child) {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);
        require(!child.isParentOf(this),
                "%s cannot be a parent and a child of %s", child, this);
        require(!this.isParentOf(child), "%s is already parent of %s!", this,
                child);

        parts.add(new ChildPart(child));
        child.parent = Optional.of(this);
        return this;
    }

    public PartialXMLElement end() {
        assumeNotEnded();
        assumePartsExist(NamePart.class, AttributesPart.class);

        parts.add(new EndPart(this.getName()));
        return this;
    }

    public String serializeCurrentContent() {
        String serialization = new String();
        for (Part part : parts) {
            if (!part.isSerialized()) {
                serialization += part.serialize();
                if (!part.isSerialized()) { // that is, if the part isn't
                                            // completely serialized yet
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
        return namePartOpt.get().getName();
    }

    public Map<String, String> getAttributes() {
        Optional<AttributesPart> attributesPartOpt = getAttributesPart();
        assumeState(attributesPartOpt.isPresent(),
                "Element's attributes not set %s", this);
        return attributesPartOpt.get().getAttributes();
    }

    public String getBody() {
        Stream<BodyPart> bodyParts = getPartsOfClassAsStream(BodyPart.class, 2);
        return bodyParts.map(body -> body.getText()).collect(joining());
    }

    public Iterable<PartialXMLElement> getChildren() {
        return getPartsOfClassAsStream(ChildPart.class).map(
                childP -> childP.getChild()).collect(toList());
    }

    public Optional<PartialXMLElement> getParent() {
        return this.parent;
    }

    public boolean isCurrentContentFullySerialized() {
        return last(parts).isSerialized();
    }

    // NOTE: either directly or indirectly
    public boolean isParentOf(PartialXMLElement child) {
        return child != this && child.getParent().isPresent()
                && getChildrenAsStream().anyMatch(myChild ->
                // TODO: comparing by identity here sounds right (since whatever
                // contents (other than children) 'child' has are unimportant),
                // but do check this!
                        myChild == child || myChild.isParentOf(child));
    }

    @SafeVarargs
    private final void assumePartsExist(Class<? extends Part>... partClasses) {
        List<Class<? extends Part>> matchedClasses = parts.stream()
                .filter(isInstanceOfAny(partClasses))
                .map(part -> part.getClass()).collect(toList());

        assumeState(matchedClasses.size() == partClasses.length,
                "Element expected parts %s to exist, but only %s exist",
                matchedClasses);
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

    private Stream<PartialXMLElement> getChildrenAsStream() {
        return getPartsOfClassAsStream(ChildPart.class).map(
                childP -> childP.getChild());
    }

    private Optional<EndPart> getEndPart() {
        if (!parts.isEmpty()) {
            Part lastPart = last(parts);
            if (lastPart instanceof EndPart) {
                return Optional.of((EndPart) lastPart);
            }
        }
        return Optional.empty();
    }

    private <P extends Part> Optional<P> getPartByIndex(Class<P> partClass,
            int i) {
        if (!parts.isEmpty() && parts.size() - 1 >= i) {
            return Optional.of(partClass.cast(parts.get(i)));
        }
        return Optional.empty();
    }

    private <P extends Part> Stream<P> getPartsOfClassAsStream(
            Class<P> partClass) {
        return parts.stream().filter(partClass::isInstance)
                .map(partClass::cast);
    }

    private <P extends Part> Stream<P> getPartsOfClassAsStream(
            Class<P> partClass, int from) {
        return parts.subList(from, parts.size()).stream()
                .filter(partClass::isInstance).map(partClass::cast);
    }

    @Override
    public String toString() {
        Stream<Part> serializedParts = parts.stream().filter(
                part -> part.isSerialized());
        Stream<Part> unserializedParts = parts.stream().filter(
                part -> !part.isSerialized());

        return serializedParts.map(part -> part.getSerialization()).collect(
                joining())
                + "\n-----Not serialized yet-----\n"
                + unserializedParts.map(part -> part.getSerialization())
                        .collect(joining());
    }
}
