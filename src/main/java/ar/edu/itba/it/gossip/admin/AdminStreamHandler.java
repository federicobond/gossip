package ar.edu.itba.it.gossip.admin;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;

public class AdminStreamHandler extends XMLStreamHandler implements XMLEventHandler {
    
    public AdminStreamHandler(AdminConversation conversation,
            InputStream fromClient, OutputStream toClient) throws XMLStreamException{
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handleEndOfInput() {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleRead(ByteBuffer buf, DeferredConnector connector) {
        //Here parse the commands?
        
        
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        // TODO Auto-generated method stub
        
    }
}
