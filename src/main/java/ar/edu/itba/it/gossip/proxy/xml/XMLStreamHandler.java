package ar.edu.itba.it.gossip.proxy.xml;

import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;

import static ar.edu.itba.it.gossip.util.Validations.assumeState;
import static com.fasterxml.aalto.AsyncXMLStreamReader.*;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public abstract class XMLStreamHandler implements TCPStreamHandler {
    // private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory
    // .newFactory();
    private static final AsyncXMLInputFactory inputFactory = new InputFactoryImpl();

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader;

    private XMLEventHandler eventHandler;
    private DeferredConnector connector;

    protected XMLStreamHandler() throws XMLStreamException {
        this.reader = newReader();
    }

    @Override
    public void handleRead(ByteBuffer buf, DeferredConnector connector) {
        this.connector = connector;
        try {
            reader.getInputFeeder().feedInput(buf);

            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                case START_DOCUMENT:
                    eventHandler.handleStartDocument(reader);
                    break;
                case END_DOCUMENT:
                    eventHandler.handleEndDocument(reader);
                    break;
                case START_ELEMENT:
                    eventHandler.handleStartElement(reader);
                    break;
                case CHARACTERS:
                    eventHandler.handleCharacters(reader);
                    break;
                case END_ELEMENT:
                    eventHandler.handleEndElement(reader);
                    break;
                }
                if (type == EVENT_INCOMPLETE) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } finally {
            this.connector = null;
        }
    }

    public void resetStream() {
        try {
            reader.close();
            reader = newReader();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> newReader()
            throws XMLStreamException {
        return inputFactory.createAsyncFor(ByteBuffer.allocate(0));
    }

    protected DeferredConnector getConnector() {
        return connector;
    }

    public void setEventHandler(final XMLEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void handleEndOfInput() {
        reader.getInputFeeder().endOfInput();
    }

    public abstract void handle(XMPPEvent event);

    protected void assumeEventType(XMPPEvent event, XMPPEvent.Type type) {
        assumeState(event.getType() == type,
                "Event type mismatch, got: %s when %s was expected", event,
                type);
    }
}
