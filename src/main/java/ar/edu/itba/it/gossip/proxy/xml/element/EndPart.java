package ar.edu.itba.it.gossip.proxy.xml.element;

import static ar.edu.itba.it.gossip.util.XMLUtils.serializeQName;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class EndPart extends Part {
    private String name;

    EndPart(final String name) {
        this.name = name;
    }

    public EndPart(AsyncXMLStreamReader<?> from) {
        this.name = serializeQName(from.getName());
    }

    @Override
    String getSerialization() {
        return "</" + name + ">";
    }

    public void setName(String newName) {
        this.name = newName;
    }
}
