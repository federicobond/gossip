package ar.edu.itba.it.gossip.proxy.xml.element;

class BodyPart extends Part {
    private final String text;

    BodyPart(final String text) {
        this.text = text;
    }

    String getText() {
        return text;
    }
    
    @Override
    String getSerialization() {
        return text;
    }
}