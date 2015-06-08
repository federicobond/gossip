package ar.edu.itba.it.gossip.admin;

import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.PASS;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.START_ADMIN;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.USER;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import ar.edu.itba.it.gossip.admin.PartialAdminElement.Type;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class AdminStreamHandler extends XMLStreamHandler implements
		XMLEventHandler {

	private PartialXMLElement xmlElement;
	private final AdminConversation conversation;
	private State state = State.INITIAL;
	private final InputStream fromClient; // Maybe it's a ByteStream
	private final OutputStream toClient;

	private String user;
	private String pass;

	public AdminStreamHandler(AdminConversation conversation,
			InputStream fromClient, OutputStream toClient)
			throws XMLStreamException {
		setXMLEventHandler(this);
		this.conversation = conversation;
		this.fromClient = fromClient;
		this.toClient = toClient;
		this.user = new String();
		this.pass = new String();
	}

	@Override
	public void handleStartElement(AsyncXMLStreamReader<?> reader) {
	    if (xmlElement == null) {
			xmlElement = new PartialXMLElement();
		} else {
			PartialXMLElement newXMLElement = new PartialXMLElement();
			xmlElement.addChild(newXMLElement);
			xmlElement = newXMLElement;
		}
		xmlElement.loadName(reader).loadAttributes(reader);

		handleStart(PartialAdminElement.from(xmlElement));
	}

	@Override
	public void handleEndElement(AsyncXMLStreamReader<?> reader) {
		xmlElement.end(reader);

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
    }

	public void handleEnd(PartialAdminElement element) {
        switch (state) {
        case READ_USER:
            user = xmlElement.getBody();
            assumeType(element, USER); // Check if this works
            state = State.EXPECT_PASS;
            sendSuccess();
            break;
        case READ_PASS:
            assumeType(element, PASS); // Check if this works
            pass = xmlElement.getBody();
            // Validate user and pass and change state depending on success
            if(user.equals("admin") && pass.equals("1234")){
                state = State.LOGGED_IN;
                sendSuccess();
            }else{
                state = State.EXPECT_USER;
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
