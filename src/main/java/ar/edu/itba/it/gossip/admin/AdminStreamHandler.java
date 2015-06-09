package ar.edu.itba.it.gossip.admin;

import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.PASS;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.START_ADMIN;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.USER;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.it.gossip.admin.PartialAdminElement.Type;
import ar.edu.itba.it.gossip.async.tcp.TCPReactorImpl;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.*;

import com.fasterxml.aalto.AsyncXMLStreamReader;

public class AdminStreamHandler extends XMLStreamHandler implements
		XMLEventHandler {

	private PartialXMLElement xmlElement;
	private final AdminConversation conversation;
	private State state = State.INITIAL;
	private final OutputStream toClient;
	private final ProxyConfig proxyConfig = ProxyConfig.getInstance();
	private final Logger logger = LoggerFactory.getLogger(TCPReactorImpl.class);
	
	private String user;
	private String pass;

	public AdminStreamHandler(AdminConversation conversation,
			InputStream fromClient, OutputStream toClient)
			throws XMLStreamException {
		setXMLEventHandler(this);
		this.conversation = conversation;
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

		try{
		    handleStart(PartialAdminElement.from(xmlElement));
		}catch (IllegalStateException e){
		    sendFail("Unexpected command");
		}
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
		    switch (element.getType()){
            case USER:
                //Looks unnecessary now
                //assumeType(element, USER);
                state = State.READ_USER;
                break ;
            case QUIT:
                quitAdmin();
                break;
            default:
                sendFail("Invalid command");
                break;
            }
            break;
		case EXPECT_PASS:
		    switch(element.getType()) {
		    case QUIT:
		        quitAdmin();
		        break;
		    case PASS:
	            state = State.READ_PASS;
	            break;
	        default:
	            sendFail("Invalid command");
	            break;
		    }
		    break;
		case LOGGED_IN:
		    switch (element.getType()){
		    case LEET:
		        state = State.READ_LEET;
		        break;
		    case SILENCE:
		        state = State.READ_SILENCE;
		        break;
		    case ORIGIN:
		        state = State.READ_ORIGIN;
		        break;
		    case STATS:
		        state = State.READ_STATS;
		        break;
		    case QUIT:
		        quitAdmin();
                break;
            default:
                sendFail("Invalid command");
                break;
		    }
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
            assumeType(element, USER);
            state = State.EXPECT_PASS;
            logger.info("User trying to log to admin > " + user);
            sendSuccess();
            break;
        case READ_PASS:
            assumeType(element, PASS);
            pass = xmlElement.getBody();
            // Validate user and pass and change state depending on success
            if (user.equals("admin") && pass.equals("1234")) {
                state = State.LOGGED_IN;
                logger.info("User logged in to admin > " + user);
                sendSuccess();
            } else {
                state = State.EXPECT_USER;
                sendFail();
            }
            break;
        case READ_LEET:
            assumeType(element,LEET);
            String leetValue = xmlElement.getBody();
            if (leetValue.toLowerCase().equals("on")) {
                proxyConfig.setLeet(true);
                logger.info("L33t conversion turned on");
                sendSuccess();
            } else if (leetValue.toLowerCase().equals("off")) {
                proxyConfig.setLeet(false);
                logger.info("L33t conversion turned off");
                sendSuccess();
            } else {
                sendFail("Wrong value");
            }
            state = State.LOGGED_IN;
            break;
        case READ_ORIGIN:
            assumeType(element,ORIGIN);
            String originUser = xmlElement.getAttributes().get("usr");
            String originAddr = xmlElement.getBody();
            proxyConfig.addOrigin(originUser, originAddr);
            logger.info("New origin added > " + originUser + " for user " + originAddr);
            sendSuccess();
            state = State.LOGGED_IN;
            break;
        case READ_SILENCE:
            assumeType(element,SILENCE);
            String silenceValue = xmlElement.getAttributes().get("value");
            String silenceUser = xmlElement.getBody();
            if (silenceValue.toLowerCase().equals("on")) {
                proxyConfig.silence(silenceUser);
                logger.info("The user < " + silenceUser + " > has been silenced");
                sendSuccess();
            }else if(silenceValue.toLowerCase().equals("off")) {
                proxyConfig.unsilence(silenceUser);
                logger.info("The user < " + silenceUser + " > has been not silenced any more");
                sendSuccess();
            }else{
                sendFail("Wrong value");
            }
            state = State.LOGGED_IN;
            break;
        case READ_STATS:
            assumeType(element,STATS);
            sendToClient("<stats type=\"" + xmlElement.getBody()+ "\">" 
                    + proxyConfig.getStats(Integer.parseInt(xmlElement.getBody())) + "</stats>\n");
            state = State.LOGGED_IN;
            break;
        case QUIT:
            state = State.INITIAL;
            break;
        case LOGGED_IN:
            break;
        default:
            sendFail();
            resetStream();
            break;
        }        
    }

    private void quitAdmin() {
        sendSuccess();
        logger.info("Admin disconected");
        conversation.quit();
        state = State.QUIT;
    }

    protected void assumeType(PartialAdminElement element, Type type) {
        assumeState(element.getType() == type,
                "Event type mismatch, got: %s when %s was expected", element,
                type);
    }

    private void sendSuccess() {
        sendToClient("<ok/>\n");
    }

    private void sendFail() {
        sendToClient("<err/>\n");
    }
    
    private void sendFail(String message) {
        sendToClient("<err>" + message + "</err>\n");
    }

    private void sendToClient(String message) {
        writeTo(toClient, message);
    }

    protected enum State {
        INITIAL, EXPECT_USER, READ_USER, READ_PASS, EXPECT_PASS, READ_ORIGIN, READ_LEET, READ_SILENCE, READ_STATS, LOGGED_IN, QUIT;
    }

}
