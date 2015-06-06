package ar.edu.itba.it.gossip.proxy.xml.element;

public class ChildPart extends Part {
    private final PartialXMLElement child;

    ChildPart(final PartialXMLElement child) {
        this.child = child;
    }

    public PartialXMLElement getChild() {
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
}
