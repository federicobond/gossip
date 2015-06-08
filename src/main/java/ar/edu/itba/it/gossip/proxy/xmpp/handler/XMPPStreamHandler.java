package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeNotSet;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.proxy.tcp.stream.TCPStream;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPEventHandler;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement.Type;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public abstract class XMPPStreamHandler extends XMLStreamHandler implements
        XMPPEventHandler, XMLEventHandler {

    private PartialXMPPElement xmppElement;

    protected boolean blocked = false;
    private TCPStream stream;
    private XMPPStreamHandler twin;

    protected XMPPStreamHandler(final TCPStream stream)
            throws XMLStreamException {
        this.stream = stream;
        setXMLEventHandler(this);
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (xmppElement == null) {
            xmppElement = PartialXMPPElement.from(reader);
        } else {
            PartialXMPPElement newXMPPElement = PartialXMPPElement.from(reader);
            this.xmppElement.addChild(newXMPPElement);
            this.xmppElement = newXMPPElement;
        }
        handleStart(xmppElement);
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmppElement.end(reader);

        handleEnd(xmppElement);

        xmppElement = xmppElement.getParent().get(); // an element that wasn't
                                                     // open will never be
                                                     // closed, since the
                                                     // underlying stream is a
                                                     // valid XML one
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmppElement.appendToBody(reader);

        handleBody(xmppElement);
    }

    protected void assumeType(PartialXMPPElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }

    protected void waitForTwin() {
        stream.pauseInflow(); // to avoid concurrency problems on the input
                              // buffer
    }

    protected void wakeUp() {
        ByteBuffer buffer = stream.getFromBuffer();
        buffer.flip(); // the buffer will be in write mode
        handleRead(buffer, null); // NOTE: so no sleeping when a
                                  // deferred connection relies
                                  // on that!
        stream.resumeInflow();
    }

    protected void wakeUpTwin() {
        twin.wakeUp();
    }

    public void setTwin(final XMPPStreamHandler twin) {
        if (this.twin == twin) {
            return;
        }
        assumeNotSet(this.twin, "Twin is already set to: %s", this.twin);
        assumeState(this.getClass() != twin.getClass(),
                "Cannot be twin of self: %s", this);
        this.twin = twin;
        twin.twin = this;
    }
}
