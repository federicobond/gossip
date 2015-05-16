package ar.edu.itba.it.gossip.proxy.xml.element;

class NamePart extends Part {
    private final String name;

    NamePart(final String name) {
        this.name = name;
    }
    
    String getName() {
        return name;
    }

    @Override
    String getSerialization() {
        return "<" + name;
    }
}
