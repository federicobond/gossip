package ar.edu.itba.it.gossip.proxy.xml.element;

class EndPart extends Part {
    final String name;

    EndPart(final String name) {
        this.name = name;
    }

    @Override
    String getSerialization() {
        return "</" + name + ">";
    }
}
