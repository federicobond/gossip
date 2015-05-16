package ar.edu.itba.it.gossip.proxy.xml;

import static ar.edu.itba.it.gossip.util.CollectionUtils.asMap;
import static ar.edu.itba.it.gossip.util.CollectionUtils.contentsAreEqual;
import static ar.edu.itba.it.gossip.util.CollectionUtils.pair;
import static ar.edu.itba.it.gossip.util.CollectionUtils.subarray;
import static ar.edu.itba.it.gossip.util.CollectionUtils.unzip;
import static ar.edu.itba.it.gossip.util.XMLUtils.serializeAttributes;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ar.edu.itba.it.gossip.proxy.xml.element.PartialXMLElement;

import com.fasterxml.aalto.AsyncXMLStreamReader;

@RunWith(MockitoJUnitRunner.class)
public class PartialXMLElementTest {
    private static final String NAME = "stream:stream";

    private static final List<Pair<String, String>> ATTRIBUTES = unmodifiableList(asList(
            pair("xmlns:stream", "'http://etherx.jabber.org/streams'"),
            pair("version", "'1.0'"), pair("from", "'localhost'"),
            pair("id", "'b5332dc6-14e9-478e-a77b-b287eac44140'"),
            pair("xml:lang", "'en'"), pair("xmlns", "'jabber:client'")));
    private static final String ATTRIBUTES_SERIALIZATION = serializeAttributes(asMap(ATTRIBUTES));

    private static final String START_TAG = "<" + NAME
            + ATTRIBUTES_SERIALIZATION + ">";

    private static final List<String> FRAGMENTED_BODY_TEXT = asList("Some ",
            "random text", " here just for testing purposes!");
    private static final String BODY_TEXT = FRAGMENTED_BODY_TEXT.stream()
            .collect(joining());

    private static final String CHILD_SERIALIZATION = "<stream:features><register xmlns='http://jabber.org/features/iq-register'/>"
            + "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
            + "<mechanism>PLAIN</mechanism></mechanisms>"
            + "</stream:features>";

    private static final String END_TAG = "</" + NAME + ">";

    @Mock
    private AsyncXMLStreamReader<?> mockReader;
    @Mock
    private PartialXMLElement mockChild;

    private PartialXMLElement sut;

    @Before
    public void setUp() {
        assert mockReader != null;

        when(mockReader.getLocalName()).thenReturn(NAME);

        when(mockReader.getAttributeCount()).thenReturn(ATTRIBUTES.size());
        Pair<List<String>, List<String>> localNamesAndValues = unzip(ATTRIBUTES);

        List<String> localNames = localNamesAndValues.getLeft();
        when(mockReader.getAttributeLocalName(anyInt())).thenReturn(
                localNames.get(0), subarray(localNames, 1));

        List<String> values = localNamesAndValues.getRight();
        when(mockReader.getAttributeValue(anyInt())).thenReturn(values.get(0),
                subarray(values, 1));

        when(mockReader.getText()).thenReturn(FRAGMENTED_BODY_TEXT.get(0),
                subarray(FRAGMENTED_BODY_TEXT, 1));

        when(mockChild.isParentOf(any(PartialXMLElement.class))).thenReturn(
                false);
        when(mockChild.getParent()).thenReturn(Optional.empty());
        when(mockChild.serializeCurrentContent()).thenReturn(
                CHILD_SERIALIZATION);
        when(mockChild.isCurrentContentFullySerialized()).thenReturn(false,
                true);

        sut = new PartialXMLElement();
    }

    @Test
    public void testSerializingEmptyElement() {
        assertEquals("", sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingName() {
        sut.loadName(mockReader);
        assertEquals(NAME, sut.getName());
    }

    @Test
    public void testSerializingJustNameTagElement() {
        sut.loadName(mockReader);
        assertEquals("<" + NAME, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingAttributes() {
        sut.loadName(mockReader);
        sut.loadAttributes(mockReader);

        assertTrue(contentsAreEqual(ATTRIBUTES, sut.getAttributes()));
    }

    @Test
    public void testSerializingFullOpenTagElement() {
        sut.loadName(mockReader).loadAttributes(mockReader);

        assertEquals(START_TAG, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);

        String body = "";
        for (String fragment : FRAGMENTED_BODY_TEXT) {
            sut.appendToBody(mockReader);
            body += fragment;

            assertEquals(body, sut.getBody());
        }
    }

    @Test
    public void testSerializingFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        appendTextToBodyInFragments();

        assertEquals(START_TAG + BODY_TEXT, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingChildrenWithFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        appendTextToBodyInFragments();

        sut.addChild(mockChild);

        assertEquals(START_TAG + BODY_TEXT + CHILD_SERIALIZATION,
                sut.serializeCurrentContent());
        verify(mockChild, times(1)).serializeCurrentContent();
    }

    @Test
    public void testSerializingEndWithFragmentedBodyAndChildren() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        appendTextToBodyInFragments();
        sut.addChild(mockChild);

        sut.end();

        assertEquals(START_TAG + BODY_TEXT + CHILD_SERIALIZATION + END_TAG,
                sut.serializeCurrentContent());
    }

    @Test
    public void testSerializationDoesNotRepeatParts() {
        sut.loadName(mockReader);
        assertEquals("<" + NAME, sut.serializeCurrentContent());

        sut.loadAttributes(mockReader);
        assertEquals(ATTRIBUTES_SERIALIZATION + ">",
                sut.serializeCurrentContent());

        appendTextToBodyInFragments();
        assertEquals(BODY_TEXT, sut.serializeCurrentContent());

        sut.addChild(mockChild);
        assertEquals(CHILD_SERIALIZATION, sut.serializeCurrentContent());

        sut.end();
        assertEquals(END_TAG, sut.serializeCurrentContent());
    }

    private void appendTextToBodyInFragments() {
        FRAGMENTED_BODY_TEXT.forEach(fragment -> sut.appendToBody(mockReader));
    }
}
