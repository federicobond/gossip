package ar.edu.itba.it.gossip.proxy.xml.element;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;

import com.fasterxml.aalto.AsyncXMLStreamReader;

class BodyPart extends Part {
    private final String text;

    BodyPart(final AsyncXMLStreamReader<?> from) {
        this.text = from.getText();
    }

    String getText() {
        return text;
    }

    @Override
    String getSerialization() {
        return escapeXml10(text);
    }
}
