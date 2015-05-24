package ar.edu.itba.it.gossip.proxy.xml.element;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import static org.apache.commons.lang3.StringEscapeUtils.*;

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
