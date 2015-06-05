package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.XMLUtils.serializeQName;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class NamePart extends Part {
    private String name;

    NamePart(final AsyncXMLStreamReader<?> from) {
        this.name = serializeQName(from.getName());
    }

    String getName() {
        return name;
    }

    void setName(final String newName) {
        this.name = newName;
    }

    @Override
    String getSerialization() {
        return "<" + name;
    }
}
