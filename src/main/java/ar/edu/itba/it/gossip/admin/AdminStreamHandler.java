package ar.edu.itba.it.gossip.admin;

import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import ar.edu.itba.it.gossip.admin.PartialAdminElement.Type;
import ar.edu.itba.it.gossip.proxy.tcp.DeferredConnector;
import ar.edu.itba.it.gossip.proxy.tcp.stream.ByteStream;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import ar.edu.itba.it.gossip.proxy.xmpp.Credentials;
import ar.edu.itba.it.gossip.proxy.xmpp.XMPPConversation;
import ar.edu.itba.it.gossip.proxy.xmpp.element.Auth;
import ar.edu.itba.it.gossip.proxy.xmpp.element.PartialXMPPElement;
import ar.edu.itba.it.gossip.util.nio.ByteBufferOutputStream;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.*;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class AdminStreamHandler extends XMLStreamHandler implements
		XMLEventHandler {

	private PartialXMLElement xmlElement;
	private final AdminConversation conversation;
	private State state = State.INITIAL;
	private final InputStream fromClient; // Maybe it's a ByteStream
	private final OutputStream toClient;

	private StringBuffer user;
	private StringBuffer pass;

	public AdminStreamHandler(AdminConversation conversation,
			InputStream fromClient, OutputStream toClient)
			throws XMLStreamException {
		setXMLEventHandler(this);
		this.conversation = conversation;
		this.fromClient = fromClient;
		this.toClient = toClient;
		this.user = new StringBuffer();
		this.pass = new StringBuffer();
	}

	@Override
	public void handleStartElement(AsyncXMLStreamReader<?> reader) {
		if (xmlElement == null) {
			xmlElement = new PartialXMLElement();
		} else {
			xmlElement = new PartialXMLElement(xmlElement);
		}
		xmlElement.loadName(reader).loadAttributes(reader);

		handleStart(PartialAdminElement.from(xmlElement));
	}

	@Override
	public void handleEndElement(AsyncXMLStreamReader<?> reader) {
		xmlElement.end();

		handleEnd(PartialAdminElement.from(xmlElement));

		xmlElement = xmlElement.getParent().get(); // an element that wasn't
													// open will never be
													// closed,
													// since the underlying
													// stream is a valid XML one
	}


	public void handleStart(PartialAdminElement element) {
		switch (state) {
		case INITIAL:
			assumeType(element, START_ADMIN);
			state = State.EXPECT_USER;
			break;
		case EXPECT_USER:
		    assumeType(element, USER);
			state = State.READ_USER;
            break;
		case EXPECT_PASS:
		    assumeType(element, PASS);
		    state = State.READ_PASS;
		    break;
		default:
			sendFail();
			resetStream();
			break;
		}
	}
	
	@Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
	    xmlElement.appendToBody(reader);

	    switch (state) {
	    case EXPECT_USER:
	        break;
	    case READ_USER:
	        user.append(xmlElement.getBody());
	        break;
	    case READ_PASS:
	        pass.append(xmlElement.getBody());
	        break;
	    default:
	        sendFail();
	        resetStream();
            break;
	    }
    }

	public void handleEnd(PartialAdminElement element) {
        switch (state) {
        case READ_USER:
            assumeType(element, USER); // Check if this works
            state = State.EXPECT_PASS;
            sendSuccess();
            break;
        case READ_PASS:
            assumeType(element, PASS); // Check if this works
            // Validate user and pass and change state depending on success
            if(user.equals("admin") && pass.equals("1234")){
                state = State.LOGGED_IN;
                sendSuccess();
            }else{
                state = State.INITIAL;
                sendFail();
            }
            break;
        default:
            sendFail();
            resetStream();
            break;
        }        
    }
	
	protected void assumeType(PartialAdminElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }

	

	protected void sendToOrigin(PartialAdminElement element) {
//		System.out.println("\n<C2O sending to origin>");
//		String currentContent = element.serializeCurrentContent();
//		System.out.println("Message:\n'"
//				+ StringEscapeUtils.escapeJava(currentContent) + "' (string) "
//				+ ArrayUtils.toString(currentContent.getBytes()));
//		sendToOrigin(currentContent);
//		System.out.println("\nOutgoing buffer afterwards:");
//		((ByteBufferOutputStream) clientToOrigin.getOutputStream())
//				.printBuffer(false, true, true);
//		System.out.println("</C2O sending to origin>\n");
	}
	
	protected void sendToOrigin(String message) {
    //    writeTo(clientToOrigin, message);
    }
	
	private void sendSuccess() {
		sendToClient("<ok/>\n");
	}
	
	private void sendFail() {
		sendToClient("<err/>\n");
	}
	
	private void sendToClient(String message) {
        writeTo(toClient, message);
    }

	protected enum State {
		INITIAL, EXPECT_USER, READ_USER, READ_PASS, EXPECT_PASS, LOGGED_IN;
	}

}
