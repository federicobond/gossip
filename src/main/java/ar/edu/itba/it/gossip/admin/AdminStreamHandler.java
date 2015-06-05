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

	public AdminStreamHandler(AdminConversation conversation,
			InputStream fromClient, OutputStream toClient)
			throws XMLStreamException {
		setXMLEventHandler(this);
		this.conversation = conversation;
		this.fromClient = fromClient;
		this.toClient = toClient;
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

//		xmlElement = xmlElement.getParent().get(); // an element that wasn't
													// open will never be
													// closed,
													// since the underlying
													// stream is a valid XML one
	}

	@Override
	public void handleCharacters(AsyncXMLStreamReader<?> reader) {
		xmlElement.appendToBody(reader);
		handleBody(PartialAdminElement.from(xmlElement));
	}

	public void handleStart(PartialAdminElement element) {
		switch (state) {
		case INITIAL:
			assumeType(element, USER);
			//sendStreamOpenToClient(originName);
			sendSuccess();
			state = State.EXPECT_PASS;
			break;
//		case VALIDATING_CREDENTIALS:
//			// FIXME: do check that the credentials were actually valid! (the
//			// code here is just assuming the client will behave and wait for an
//			// auth <success>).
//			assumeType(element, STREAM_START);
//			state = LINKED;
//			System.out
//					.println("Client is linked to origin, now messages may pass freely");
//
//			sendDocumentStartToOrigin();
//			// fall through
//		case LINKED:
//			sendToOrigin(element);
//			break;
//		default:
//			// do nothing TODO: change this!
		default:
			sendFail();
			break;
		}
	}
	
	protected void assumeType(PartialAdminElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }

	public void handleEnd(PartialAdminElement element) {
		switch (state) {
//		case EXPECT_USER:
//			assumeType(element, AUTH_CHOICE);
//			Credentials credentials = ((Auth) element).getCredentials();
//			conversation.setCredentials(credentials);
//			System.out.println(credentials.getUsername()
//					+ " is trying to log in with password: "
//					+ credentials.getPassword());
//			connectToOrigin();
//
//			// Update the origin name because might be changed by
//			// multiplexation.
//			originName = proxyConfig.getOriginName(credentials.getUsername());
//			sendStreamOpenToOrigin(originName);
//
//			resetStream();
//
//			state = VALIDATING_CREDENTIALS;
//			break;
//		case LINKED:
//			sendToOrigin(element);
//			break;
//		default:
//			// will never happen
//			throw new IllegalStateException("Unexpected state" + state);
		}
	}

	public void handleBody(PartialAdminElement element) {
		if (state == State.LOGGED_IN) {
			sendToOrigin(element);
		}
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
		sendToClient("<ok/>");
	}
	
	private void sendFail() {
		sendToClient("<err/>");
	}
	
	private void sendToClient(String message) {
        writeTo(toClient, message);
    }

	protected enum State {
		INITIAL, EXPECT_PASS, LOGGED_IN;
	}

}
