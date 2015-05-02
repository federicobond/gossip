package ar.edu.itba.it.gossip.xmpp;

import ar.edu.itba.it.gossip.xml.EventHandler;
import ar.edu.itba.it.gossip.xmpp.event.Event;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class StreamHandler {
    protected static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    protected static final AsyncXMLInputFactory INPUT_FACTORY = new InputFactoryImpl();

    protected AsyncXMLStreamReader<AsyncByteBufferFeeder> reader;

    protected OutputStream client;
    protected OutputStream origin;

    private EventHandler eventHandler;

    public void read(ByteBuffer buf) {
        try {
            reader.getInputFeeder().feedInput(buf);

            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        eventHandler.onStartDocument(reader);
                        break;
                    case AsyncXMLStreamReader.END_DOCUMENT:
                        eventHandler.onEndDocument(reader);
                        break;
                    case AsyncXMLStreamReader.START_ELEMENT:
                        eventHandler.onStartElement(reader);
                        break;
                    case AsyncXMLStreamReader.CHARACTERS:
                        eventHandler.onCharacters(reader);
                        break;
                    case AsyncXMLStreamReader.END_ELEMENT:
                        eventHandler.onEndElement(reader);
                        break;
                }
                if (type == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetStream() {
        try {
            reader.close();
            reader = INPUT_FACTORY.createAsyncFor(ByteBuffer.allocate(0));
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEventHandler(EventHandler handler) {
        this.eventHandler = handler;
    }

    public void endOfInput() {
        reader.getInputFeeder().endOfInput();
    }

    public abstract void processEvent(Event event);
}
