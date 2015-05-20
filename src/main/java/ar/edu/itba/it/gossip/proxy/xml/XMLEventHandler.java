package ar.edu.itba.it.gossip.proxy.xml;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public interface XMLEventHandler {
    default void handleStartDocument(AsyncXMLStreamReader<?> reader) {}

    default void handleEndDocument(AsyncXMLStreamReader<?> reader) {}

    void handleStartElement(AsyncXMLStreamReader<?> reader);

    void handleEndElement(AsyncXMLStreamReader<?> reader);

    void handleCharacters(AsyncXMLStreamReader<?> reader);
}
