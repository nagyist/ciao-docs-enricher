package uk.nhs.ciao.docs.enricher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Maps;

import uk.nhs.ciao.docs.parser.Document;
import uk.nhs.ciao.docs.parser.ParsedDocument;

/**
 * Unit tests for {@link DynamicDocumentEnricher}
 */
public class DynamicDocumentEnricherTest {
	private DynamicDocumentEnricher enricher;
	private DocumentPropertiesFinder finder;
	
	@Before
	public void setup() {
		finder = Mockito.mock(DocumentPropertiesFinder.class, Mockito.RETURNS_DEFAULTS); // empty map by default
		enricher = new DynamicDocumentEnricher(finder);
		
		
	}
	
	private ParsedDocument createDocument() {
		final Map<String, Object> properties = Maps.newLinkedHashMap();
		
		properties.put("name", "example");
		properties.put("id", "12");
		properties.put("authors", Arrays.<Object>asList(
				person("2"),
				person("5")));
		properties.put("versions", Arrays.asList(10, 22, 31));
		
		return new ParsedDocument(new Document("example.txt", "hello world".getBytes()), properties);
	}
	
	private Map<String, Object> person(final String id) {
		return map("id", id);
	}
	
	private Map<String, Object> person(final String id, final String name) {
		final Map<String, Object> person = person(id);
		person.put("name", name);
		return person;
	}
	
	private Map<String, Object> map(final String key, final Object value) {
		final Map<String, Object> map = Maps.newLinkedHashMap();		
		map.put(key, value);
		return map;
	}
	
	@Test
	public void testKeySelectors() throws Exception {
		enricher.setLookupKeySelectors(Arrays.asList("authors[0]"));
		final Map<String, Object> expectedKeys = Maps.newLinkedHashMap();
		expectedKeys.put("authors[0]", person("2"));
		
		enricher.enrichDocument(createDocument());
		
		Mockito.verify(finder).findProperties(expectedKeys);
		
		enricher.setLookupKeySelectors(Arrays.asList("authors[*]"));
		expectedKeys.clear();
		expectedKeys.put("authors[0]", person("2"));
		expectedKeys.put("authors[1]", person("5"));
		
		enricher.enrichDocument(createDocument());
		
		Mockito.verify(finder).findProperties(expectedKeys);
	}
	
	@Test
	public void testPropertySelectors() throws Exception {
		enricher.setLookupKeySelectors(Arrays.asList("id"));
		enricher.setEnrichablePropertiesSelectors(Arrays.asList("authors[*]"));
		
		Mockito.when(finder.findProperties(person("2"))).thenReturn(person("2", "John Smith"));
		Mockito.when(finder.findProperties(person("5"))).thenReturn(person("5", "Peter Jones"));
		
		final ParsedDocument document = enricher.enrichDocument(createDocument());
		
		final List<?> authors = (List<?>)document.getProperties().get("authors");
		Assert.assertEquals(2, authors.size());
		Assert.assertEquals(authors.get(0), person("2", "John Smith"));
		Assert.assertEquals(authors.get(1), person("5", "Peter Jones"));
	}
}
