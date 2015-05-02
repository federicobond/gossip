package ar.edu.itba.it.gossip.xml;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public interface EventHandler {

    void onStartDocument(AsyncXMLStreamReader<?> reader);

    void onEndDocument(AsyncXMLStreamReader<?> reader);

    void onStartElement(AsyncXMLStreamReader<?> reader);

    void onEndElement(AsyncXMLStreamReader<?> reader);

    void onCharacters(AsyncXMLStreamReader<?> reader);
}
