package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.XMLUtils.serializeQName;

import com.fasterxml.aalto.AsyncXMLStreamReader;

class NamePart extends Part {
    private final String name;

    NamePart(final AsyncXMLStreamReader<?> from) {
        this.name = serializeQName(from.getName());
    }

    String getName() {
        return name;
    }

    @Override
    String getSerialization() {
        return "<" + name;
    }
}
