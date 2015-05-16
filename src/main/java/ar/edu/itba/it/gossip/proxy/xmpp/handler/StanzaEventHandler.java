package ar.edu.itba.it.gossip.proxy.xmpp.handler;

import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_CHOICE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FAILURE;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_FEATURES;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISM;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_MECHANISMS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_REGISTER;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.AUTH_SUCCESS;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.OTHER;
import static ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent.Type.STREAM_START;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.event.XMPPEvent;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class StanzaEventHandler implements XMLEventHandler {
    private final XMPPStreamHandler handler;

    private PartialXMLElement element;

    public StanzaEventHandler(XMPPStreamHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handleStartDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleEndDocument(AsyncXMLStreamReader<?> reader) {
    }

    @Override
    public void handleStartElement(AsyncXMLStreamReader<?> reader) {
        if (element == null) {
            element = new PartialXMLElement();
        } else {
            PartialXMLElement newElement = new PartialXMLElement(element);
            this.element = newElement;
        }
        element.loadName(reader).loadAttributes(reader);

        XMPPEvent event = XMPPEvent.from(getEventType(element.getName()),
                element);
        handler.handle(event);
    }

    private XMPPEvent.Type getEventType(String name) {
        switch (name) {
        // client
        case "auth":
            return AUTH_CHOICE;
            // client

            // origin
        case "features":
            return AUTH_FEATURES;
        case "register":
            return AUTH_REGISTER;
        case "mechanisms":
            return AUTH_MECHANISMS;
        case "mechanism":
            return AUTH_MECHANISM;

        case "success":
            return AUTH_SUCCESS;
        case "failure":
            return AUTH_FAILURE;
            // origin

            // both
        case "stream":
            // TODO: verify which stream open this is!
            // (also whether it is the first or second!)
            return STREAM_START;
        default:
            return OTHER;
            // both
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        // final XMPPEvent event;
        // switch (element.getName()) {
        // case "auth":
        // event = XMPPEvent.from(AUTH, element);
        // this.state = INITIAL;
        // break;
        // case "response":
        // event = XMPPEvent.from(RESPONSE, element);
        // this.state = INITIAL;
        // break;
        //
        // // TODO
        // case "register":
        // event = XMPPEvent.from(AUTH_REGISTER, element);
        // break;
        // case "mechanisms":
        // event = XMPPEvent.from(AUTH_MECHANISMS, element);
        // break;
        // case "mechanism":
        // event = XMPPEvent.from(AUTH_MECHANISM, element);
        // break;
        // case "features":
        // event = XMPPEvent.from(AUTH_FEATURES_END, element);
        // this.state = INITIAL;
        // break;
        // // TODO
        //
        // // these DON'T HAPPEN INSIDE AUTH_FEATURES
        // case "success":
        // event = XMPPEvent.from(AUTH_SUCCESS, element);
        // break;
        // case "failure":
        // event = XMPPEvent.from(AUTH_FAILURE, element);
        // break;
        // // TODO
        //
        // default:
        // throw new RuntimeException("unknown element: " + element.getName());
        // }

        // element = null;
        // handler.handle(event);
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        // llamar al handler padre para que mande la orden de appendear
        element.appendToBody(reader);
    }
}
