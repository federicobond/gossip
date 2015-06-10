package ar.edu.itba.it.gossip.admin;

import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.LEET;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.ORIGIN;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.PASS;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.SILENCE;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.START_ADMIN;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.STATS;
import static ar.edu.itba.it.gossip.admin.PartialAdminElement.Type.USER;
import static ar.edu.itba.it.gossip.util.ValidationUtils.assumeState;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.it.gossip.admin.PartialAdminElement.Type;
import ar.edu.itba.it.gossip.async.tcp.TCPReactorImpl;
import ar.edu.itba.it.gossip.proxy.configuration.ProxyConfig;
import ar.edu.itba.it.gossip.proxy.xml.XMLEventHandler;
import ar.edu.itba.it.gossip.proxy.xml.XMLStreamHandler;
import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

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

        try {
            handleStart(PartialAdminElement.from(xmlElement));
        } catch (IllegalStateException e) {
            sendFailure(104, "Unexpected tag");
            quitAdmin();
        }
    }

    @Override
    public void handleEndElement(AsyncXMLStreamReader<?> reader) {
        xmlElement.end(reader);

        handleEnd(PartialAdminElement.from(xmlElement));

        try{
            xmlElement = xmlElement.getParent().get();
        }catch (NoSuchElementException e){
            // We do nothing because we have already sent an error
        }
         
    }

    private boolean adminLogin(String user, String password) {
        String adminUser = proxyConfig.getAdminUser();
        String adminPassword = proxyConfig.getAdminPassword();

        return user.equals(adminUser) && password.equals(adminPassword);
    }

    public void handleStart(PartialAdminElement element) {
        switch (state) {
        case INITIAL:
            if(element.getType() != START_ADMIN){
                sendFailure(101, "Unrecognized tag");
                quitAdmin();
                break;
            }
            state = State.EXPECT_USER;
            break;
        case EXPECT_USER:
            switch (element.getType()) {
            case USER:
                state = State.READ_USER;
                break;
            case QUIT:
                sendSuccess();
                quitAdmin();
                break;
            default:
                sendFailure(101, "Unrecognized tag");
                break;
            }
            break;
        case EXPECT_PASS:
            switch (element.getType()) {
            case QUIT:
                sendSuccess();
                quitAdmin();
                break;
            case PASS:
                state = State.READ_PASS;
                break;
            default:
                sendFailure(101, "Unrecognized tag");
                break;
            }
            break;
        case LOGGED_IN:
            switch (element.getType()) {
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
                sendSuccess();
                quitAdmin();
                break;
            default:
                sendFailure(101, "Unrecognized tag");
                break;
            }
            break;
        default:
            sendFailure(104, "Unknown error");
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
            if (adminLogin(user, pass)) {
                state = State.LOGGED_IN;
                logger.info("User logged in to admin > " + user);
                sendSuccess();
            } else {
                state = State.EXPECT_USER;
                sendFailure(105, "Wrong admin credentials");
            }
            break;
        case READ_LEET:
            assumeType(element, LEET);
            String leetValue = xmlElement.getBody().toLowerCase();

            switch (leetValue) {
            case "on":
                proxyConfig.setLeet(true);
                logger.info("L33t conversion turned on");
                sendSuccess();
                break;
            case "off":
                proxyConfig.setLeet(false);
                logger.info("L33t conversion turned off");
                sendSuccess();
                break;
            default:
                sendFailure(103, "Wrong leet value");
            }
            state = State.LOGGED_IN;
            break;
        case READ_ORIGIN:
            assumeType(element, ORIGIN);
            String originUser = xmlElement.getAttributes().get("usr");
            String originAddr = xmlElement.getBody();
            proxyConfig.addOriginMapping(originUser, originAddr);
            logger.info("New origin added > " + originUser + " for user "
                    + originAddr);
            sendSuccess();
            state = State.LOGGED_IN;
            break;
        case READ_SILENCE:
            assumeType(element, SILENCE);
            String silenceValue = xmlElement.getAttributes().get("value")
                    .toLowerCase();
            String silenceUser = xmlElement.getBody();

            switch (silenceValue) {
            case "on":
                proxyConfig.silence(silenceUser);
                logger.info("The user < " + silenceUser
                        + " > has been silenced");
                sendSuccess();
                break;
            case "off":
                proxyConfig.unsilence(silenceUser);
                logger.info("The user < " + silenceUser
                        + " > has been not silenced any more");
                sendSuccess();
                break;
            default:
                sendFailure(102, "Wrong silence value");
            }
            state = State.LOGGED_IN;
            break;
        case READ_STATS:
            assumeType(element, STATS);
            getStats(Integer.parseInt(xmlElement.getBody()));
            break;
        case QUIT:
            state = State.INITIAL;
            break;
        case LOGGED_IN:
            break;
        default:
            break;
        }
    }

    @Override
    public void handleError(Exception e) {
        sendFailure(100, "Malformed XML input");
        conversation.quit();
        state = State.QUIT;
    }

    private void getStats(int option) {
        state = State.LOGGED_IN;
        switch (option) {
        case 1:
            sendToClient("<stats> \n\t <type>" + option + "</type> \n"
                    + "\t <desc>Number of read bytes</desc> \n" + "\t <value>"
                    + proxyConfig.getReadBytes() + "</value>\n" + "</stats>\n");
            return;
        case 2:
            sendToClient("<stats> \n\t <type>" + option + "</type> \n"
                    + "\t <desc>Number of written bytes</desc> \n"
                    + "\t <value>" + proxyConfig.getWrittenBytes()
                    + "</value>\n" + "</stats>\n");
            return;
        case 3:
            sendToClient("<stats> \n\t <type>" + option + "</type> \n"
                    + "\t <desc>Number of connections to proxy</desc> \n"
                    + "\t <value>" + proxyConfig.getAccesses() + "</value>\n"
                    + "</stats>\n");
            return;
        case 4:
            sendToClient("<stats> \n\t <type>"
                    + option
                    + "</type> \n"
                    + "\t <desc>Number of messages sent through proxy</desc> \n"
                    + "\t <value>" + proxyConfig.getSentMessagesCount()
                    + "</value>\n" + "</stats>\n");
            return;
        case 5:
            sendToClient("<stats> \n\t <type>"
                    + option
                    + "</type> \n"
                    + "\t <desc>Number of messages received through proxy</desc> \n"
                    + "\t <value>" + proxyConfig.getReceivedMessagesCount()
                    + "</value>\n" + "</stats>\n");
            return;
        }
        sendFailure(201, "Invalid statistics option");
        return;
    }

    private void quitAdmin() {
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
        sendToClient("<success/>\n");
    }

    private void sendFailure(int code, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("<error>\n\t<code>");
        sb.append(code);
        sb.append("</code>\n\t<message>");
        sb.append(message);
        sb.append("</message>\n</error>\n");
        sendToClient(sb.toString());
    }

    private void sendToClient(String message) {
        writeTo(toClient, message);
    }

    @Override
    public void handleError(XMLStreamException exc) {
        handleError(new Exception());
    }

    protected enum State {
        INITIAL, EXPECT_USER, READ_USER, READ_PASS, EXPECT_PASS, READ_ORIGIN, READ_LEET, READ_SILENCE, READ_STATS, LOGGED_IN, QUIT;
    }
}
