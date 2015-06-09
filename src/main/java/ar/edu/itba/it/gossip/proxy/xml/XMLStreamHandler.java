package ar.edu.itba.it.gossip.proxy.xml;

import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.TCPStreamHandler;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public abstract class XMLStreamHandler implements TCPStreamHandler {
    private static final byte LT = 0x3C;
    private static final byte GT = 0x3E;

    private static final AsyncXMLInputFactory inputFactory = new InputFactoryImpl();

    private XMLEventHandler xmlHandler;

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader;
    private DeferredConnector connector;
    private int gtsRead = 0;
    private boolean paused = false;

    protected XMLStreamHandler() throws XMLStreamException {
        this.reader = newReader();
    }

    @Override
    public void handleRead(ByteBuffer buf, final DeferredConnector connector) {
        this.connector = connector;
        try {
            reader.getInputFeeder().feedInput(buf);

            while (!paused && reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                case START_DOCUMENT:
                    gtsRead++;
                    xmlHandler.handleStartDocument(reader);
                    break;
                case END_DOCUMENT:
                    xmlHandler.handleEndDocument(reader);
                    break;
                case START_ELEMENT:
                    gtsRead++;
                    xmlHandler.handleStartElement(reader);
                    break;
                case CHARACTERS:
                    xmlHandler.handleCharacters(reader);
                    break;
                case END_ELEMENT:
                    gtsRead++;
                    xmlHandler.handleEndElement(reader);
                    break;
                }
                if (type == EVENT_INCOMPLETE) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            xmlHandler.handleError(e);
        }
        this.connector = null;

        buf.position(getNewPosition(buf));
        buf.compact();
    }

    @Override
    public void handleError(Exception ex) {
        throw new RuntimeException(ex);
    }

    protected int getNewPosition(ByteBuffer buffer) { // because thank you aalto
                                                      // for not showing offsets
        // IMPORTANT: this will NOT work with CDATAs NOR COMMENTS!
        byte[] bytes = buffer.array();
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            switch (bytes[i]) {
            case LT:
                if (gtsRead == 0) { // "(...<...>)*<..." case
                    return i; // return last <'s position
                }
                break;
            case GT:
                if (gtsRead > 0) { // "...>X" -> X's position
                    gtsRead--;
                }
                break;
            default:
                // do nothing
            }
        }

        // "(...<...>)*..." case
        return buffer.limit();
    }

    protected void resetStream() {
        try {
            reader.close();
            reader = newReader();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void pause() {
        this.paused = true;
    }

    protected void resume() {
        this.paused = false;
    }

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> newReader()
            throws XMLStreamException {
        return inputFactory.createAsyncFor(ByteBuffer.allocate(0));
    }

    protected DeferredConnector getConnector() {
        return connector;
    }

    public void setXMLEventHandler(final XMLEventHandler eventHandler) {
        this.xmlHandler = eventHandler;
    }

    @Override
    public void handleEndOfInput() {
        reader.getInputFeeder().endOfInput();
    }

    protected void writeTo(OutputStream stream, String payload) {
        try {
            stream.write(payload.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
