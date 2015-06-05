package ar.edu.itba.it.gossip.proxy.xml.element;

public class EndPart extends Part {
    private String name;

    EndPart(final String name) {
        this.name = name;
    }

    @Override
    String getSerialization() {
        return "</" + name + ">";
    }

    public void setName(String newName) {
        this.name = newName;
    }
}
