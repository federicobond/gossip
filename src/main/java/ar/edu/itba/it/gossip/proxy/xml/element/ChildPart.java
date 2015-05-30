package ar.edu.itba.it.gossip.proxy.xml.element;

import java.util.function.Function;

class ChildPart extends Part {
    private final PartialXMLElement child;

    ChildPart(final PartialXMLElement child) {
        this.child = child;
    }

    PartialXMLElement getChild() {
        return child;
    }

    @Override
    String serialize() {
        return child.serializeCurrentContent();
    }

    @Override
    boolean isSerialized() {
        return child.isCurrentContentFullySerialized();
    }

    @Override
    String getSerialization() {
        return child.getSerialization();
    }

    void setBodyTransformation(Function<String, String> transformation) {
        this.child.setBodyTransformation(transformation);
    }
}
