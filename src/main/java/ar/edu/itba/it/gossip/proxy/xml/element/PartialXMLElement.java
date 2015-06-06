package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.CollectionUtils.last;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;
import static ar.edu.itba.it.gossip.util.ValidationUtils.require;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import ar.edu.itba.it.gossip.util.PartiallySerializable;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class PartialXMLElement implements PartiallySerializable {
    private Optional<PartialXMLElement> parent;
    private final List<Part> parts;

    private boolean startTagEnded = false;
    private boolean ended = false;

    public PartialXMLElement() {
        this.parts = new LinkedList<>();
        this.parent = Optional.empty();
    }

    public PartialXMLElement loadName(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeState(!getNamePart().isPresent(), "%s already has a name", this);

        parts.add(new NamePart(from));
        return this;
    }

    public PartialXMLElement loadAttributes(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeState(getNamePart().isPresent(), "%s doesn't have a name", this);
        assumeState(!getAttributesPart().isPresent(),
                "%s already has attributes", this);

        parts.add(new AttributesPart(from));
        startTagEnded = true;
        return this;
    }

    public PartialXMLElement appendToBody(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeStartTagEnded();

        parts.add(new BodyPart(from));
        return this;
    }

    public PartialXMLElement addChild(PartialXMLElement child) {
        assumeNotEnded();
        assumeStartTagEnded();
        require(!child.isParentOf(this),
                "%s cannot be a parent and a child of %s", child, this);
        require(!this.isParentOf(child), "%s is already parent of %s!", this,
                child);

        parts.add(new ChildPart(child));
        child.parent = Optional.of(this);
        return this;
    }

    public PartialXMLElement end(AsyncXMLStreamReader<?> from) {
        assumeNotEnded();
        assumeStartTagEnded();

        parts.add(new EndPart(from));
        ended = true;
        return this;
    }

    @Override
    public String serializeCurrentContent() {
        String serialization = new String();
        List<Part> toRemove = new LinkedList<Part>();
        for (Part part : parts) {
            if (!part.isSerialized()) { // IMPORTANT: ChildParts could not be
                                        // serialized and thus appear here!
                serialization += serialize(part);
                if (!part.isSerialized()) { // that is, if the part still isn't
                                            // completely serialized
                    return serialization;
                }
            }
            toRemove.add(part);
        }
        parts.removeAll(toRemove);
        return serialization;
    }

    protected String serialize(Part part) { // to be overriden by subclasses
        return part.serialize();
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

    public Map<String, String> getNamespaces() {
        Optional<AttributesPart> attributesPartOpt = getAttributesPart();
        assumeState(attributesPartOpt.isPresent(),
                "Element's namespaces not set %s", this);
        return attributesPartOpt.get().getNamespaces();
    }

    public String getBody() {
        Stream<BodyPart> bodyParts = getPartsOfClassAsStream(BodyPart.class);
        return bodyParts.map(body -> body.getText()).collect(joining());
    }

    public Iterable<? extends PartialXMLElement> getChildren() {
        return getPartsOfClassAsStream(ChildPart.class).map(
                childP -> childP.getChild()).collect(toList());
    }

    public Optional<? extends PartialXMLElement> getParent() {
        return this.parent;
    }

    protected void modifyName(String newName) {
        assumeState(startTagEnded, "Element's name is not set %s", this);
        Optional<NamePart> namePartOpt = getNamePart();
        if (namePartOpt.isPresent()) {
            namePartOpt.get().setName(newName);
        }
        if (ended) {
            Optional<EndPart> endPartOpt = getEndPart();
            endPartOpt.get().setName(newName);
        }
        // NOTE: it is ok not to fail, 'this' may have an end (yet)!
    }

    protected boolean isCurrentContentFullySerialized() {
        return parts.isEmpty() && ended;
    }

    protected boolean isParentOf(PartialXMLElement child) { // NOTE: either
                                                            // directly or
                                                            // indirectly
        return child != this && child.getParent().isPresent()
                && getChildrenAsStream().anyMatch(myChild ->
                // TODO: comparing by identity here sounds right (since whatever
                // contents (other than children) 'child' has are unimportant),
                // but do check this!
                        myChild == child || myChild.isParentOf(child));
    }

    private Stream<PartialXMLElement> getChildrenAsStream() {
        return getPartsOfClassAsStream(ChildPart.class).map(
                childP -> childP.getChild());
    }

    private Optional<NamePart> getNamePart() {
        return getPartsOfClassAsStream(NamePart.class).findFirst();
    }

    private Optional<AttributesPart> getAttributesPart() {
        return getPartsOfClassAsStream(AttributesPart.class).findFirst();
    }

    private <P extends Part> Stream<P> getPartsOfClassAsStream(
            Class<P> partClass) {
        return parts.stream().filter(partClass::isInstance)
                .map(partClass::cast);
    }

    private void assumeStartTagEnded() {
        assumeState(startTagEnded, "%s doesn't have a start tag", this);
    }

    private void assumeNotEnded() {
        assumeState(!ended, "Element already ended %s", this);
    }

    private Optional<EndPart> getEndPart() {
        assumeState(ended, "%s hasn't ended yet", this);
        if (!parts.isEmpty()) {
            Part lastPart = last(parts);
            if (lastPart instanceof EndPart) {
                return Optional.of((EndPart) lastPart);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        Stream<Part> unserializedParts = parts.stream();
        String yetToSerialize = unserializedParts.map(
                part -> getSerialization(part)).collect(joining());

        return "\n-----Not serialized yet-----\n" + yetToSerialize;
    }

    private String getSerialization(Part part) { // TODO: test method! remove
                                                 // this later
        return part.getSerialization();
    }
}
