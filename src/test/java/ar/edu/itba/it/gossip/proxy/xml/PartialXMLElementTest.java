package ar.edu.itba.it.gossip.proxy.xml;

import static ar.edu.itba.it.gossip.util.CollectionUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ar.edu.itba.it.gossip.util.XMLUtils;

import com.fasterxml.aalto.AsyncXMLStreamReader;

@RunWith(MockitoJUnitRunner.class)
public class PartialXMLElementTest {
    private static final String STREAM_OPEN_NAME = "stream:stream";

    private static final List<Pair<String, String>> streamOpenAttributes = unmodifiableList(asList(
            pair("xmlns:stream", "'http://etherx.jabber.org/streams'"),
            pair("version", "'1.0'"), pair("from", "'localhost'"),
            pair("id", "'b5332dc6-14e9-478e-a77b-b287eac44140'"),
            pair("xml:lang", "'en'"), pair("xmlns", "'jabber:client'")));

    private static final String ATTRIBUTES_SERIALIZATION = XMLUtils
            .serializeAttributes(asMap(streamOpenAttributes));

    private static final String STREAM_OPEN = "<" + STREAM_OPEN_NAME
            + ATTRIBUTES_SERIALIZATION + ">";

    private static final List<String> textFragments = asList("Some ",
            "random text", " here just for testing purposes!");
    private static final String TEXT = textFragments.stream().collect(
            Collectors.joining());

    private static final String CHILD_SERIALIZATION = "<stream:features><register xmlns='http://jabber.org/features/iq-register'/>"
            + "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
            + "<mechanism>PLAIN</mechanism></mechanisms>"
            + "</stream:features>";

    private static final String STREAM_OPEN_END = "</" + STREAM_OPEN_NAME + ">";

    @Mock
    private AsyncXMLStreamReader<?> mockReader;
    @Mock
    private PartialXMLElement mockChild;

    private PartialXMLElement sut;

    @Before
    public void setUp() {
        assert mockReader != null;

        when(mockReader.getLocalName()).thenReturn(STREAM_OPEN_NAME);

        when(mockReader.getAttributeCount()).thenReturn(
                streamOpenAttributes.size());
        Pair<List<String>, List<String>> localNamesAndValues = unzip(streamOpenAttributes);

        List<String> localNames = localNamesAndValues.getLeft();
        when(mockReader.getAttributeLocalName(anyInt())).thenReturn(
                localNames.get(0), subarray(localNames, 1));

        List<String> values = localNamesAndValues.getRight();
        when(mockReader.getAttributeValue(anyInt())).thenReturn(values.get(0),
                subarray(values, 1));

        when(mockReader.getText()).thenReturn(textFragments.get(0),
                subarray(textFragments, 1));

        when(mockChild.isParentOf(any(PartialXMLElement.class))).thenReturn(false);
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
        assertEquals(STREAM_OPEN_NAME, sut.getName());
    }

    @Test
    public void testSerializingJustNameTagElement() {
        sut.loadName(mockReader);
        assertEquals("<" + STREAM_OPEN_NAME, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingAttributes() {
        sut.loadName(mockReader);
        sut.loadAttributes(mockReader);

        assertTrue(contentsAreEqual(streamOpenAttributes, sut.getAttributes()));
    }

    @Test
    public void testSerializingFullOpenTagElement() {
        sut.loadName(mockReader).loadAttributes(mockReader);

        assertEquals(STREAM_OPEN, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);

        String body = "";
        for (String fragment : textFragments) {
            sut.appendToBody(mockReader);
            body += fragment;

            assertEquals(body, sut.getBody());
        }
    }

    @Test
    public void testSerializingFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        textFragments.forEach(fragment -> sut.appendToBody(mockReader));

        assertEquals(STREAM_OPEN + TEXT, sut.serializeCurrentContent());
    }

    @Test
    public void testLoadingChildrenWithFragmentedBody() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        textFragments.forEach(fragment -> sut.appendToBody(mockReader));

        sut.addChild(mockChild);

        assertEquals(STREAM_OPEN + TEXT + CHILD_SERIALIZATION,
                sut.serializeCurrentContent());
        verify(mockChild, times(1)).serializeCurrentContent();
    }

    @Test
    public void testSerializingEndWithFragmentedBodyAndChildren() {
        sut.loadName(mockReader).loadAttributes(mockReader);
        textFragments.forEach(fragment -> sut.appendToBody(mockReader));
        sut.addChild(mockChild);

        sut.end();

        assertEquals(
                STREAM_OPEN + TEXT + CHILD_SERIALIZATION + STREAM_OPEN_END,
                sut.serializeCurrentContent());
    }

    @Test
    public void testSerializationDoesNotRepeatParts() {
        sut.loadName(mockReader);
        assertEquals("<" + STREAM_OPEN_NAME, sut.serializeCurrentContent());

        sut.loadAttributes(mockReader);
        assertEquals(ATTRIBUTES_SERIALIZATION + ">",
                sut.serializeCurrentContent());

        textFragments.forEach(fragment -> sut.appendToBody(mockReader));
        assertEquals(TEXT, sut.serializeCurrentContent());

        sut.addChild(mockChild);
        assertEquals(CHILD_SERIALIZATION, sut.serializeCurrentContent());

        sut.end();
        assertEquals(STREAM_OPEN_END, sut.serializeCurrentContent());
    }
}
