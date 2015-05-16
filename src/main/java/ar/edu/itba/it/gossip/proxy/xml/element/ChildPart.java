package ar.edu.itba.it.gossip.proxy.xml.element;

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
        String serialization = getSerialization();
        return serialization;
    }

    @Override
    boolean isSerialized() {
        return child.isCurrentContentFullySerialized();
    }

    @Override
    String getSerialization() {
        return child.serializeCurrentContent();
    }
}
