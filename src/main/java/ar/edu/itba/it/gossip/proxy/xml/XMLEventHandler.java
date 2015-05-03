package ar.edu.itba.it.gossip.proxy.xml;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public interface XMLEventHandler {
    void handleStartDocument(AsyncXMLStreamReader<?> reader);

    void handleEndDocument(AsyncXMLStreamReader<?> reader);

    void handleStartElement(AsyncXMLStreamReader<?> reader);

    void handleEndElement(AsyncXMLStreamReader<?> reader);

    void handleCharacters(AsyncXMLStreamReader<?> reader);
}
