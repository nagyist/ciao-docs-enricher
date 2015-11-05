package uk.nhs.ciao.docs.enricher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

/**
 * Unit tests for {@link FileDocumentPropertiesFinder}
 */
public class FileDocumentPropertiesFinderTest {
	private ObjectMapper objectMapper;
	private MockFilesystem filesystem;
	private FileDocumentPropertiesFinder finder;
	private Map<String, Object> properties;
	private Map<String, Object> additional;
	private String additionalJson;
	
	@Before
	public void setup() throws Exception {
		objectMapper = new ObjectMapper();
		// Interaction with file-system is mocked out
		filesystem = Mockito.spy(new MockFilesystem());
		finder = new FileDocumentPropertiesFinder(objectMapper) {
			@Override
			protected InputStream getJson(final String parent, final String fileName) throws IOException {
				return filesystem.get(parent, fileName);
			}
		};
		
		properties = Maps.newLinkedHashMap();
		properties.put("name", "original name");
		properties.put("description", "some text");
		properties.put("id", "123");
		
		additional = Maps.newLinkedHashMap();
		additional.put("added-name", "Additional Name");
		additional.put("added-address", "Additional Address");
		
		final Map<String, Object> nested = Maps.newLinkedHashMap();
		nested.put("nested-name", "Nested Name");
		nested.put("nested-address", "Nested Address");
		additional.put("123", nested);
		
		additionalJson = objectMapper.writeValueAsString(additional);
	}
	
	public static class MockFilesystem {
		private final Map<String, String> map = Maps.newHashMap();
		
		private String key(final String parent, final String fileName) {
			return parent == null ? fileName : parent + "|||" + fileName;
		}
		
		public void put(final String parent, final String fileName, final String value) {
			map.put(key(parent, fileName), value);
		}
		
		public InputStream get(final String parent, final String fileName) {
			final String value = map.get(key(parent, fileName));
			return value == null ? null : new ByteArrayInputStream(value.getBytes());
		}
	}
	
	@Test
	public void testStaticFileName() throws Exception {
		filesystem.put(null, "static-file.json", additionalJson);
		
		finder.setFilePath("static-file.json");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testDynamicFileName() throws Exception {
		filesystem.put(null, "original name.json", additionalJson);
		
		finder.setFileNameSelector("name");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testFilePathWithDynamicFileName() throws Exception {
		filesystem.put("/static-root", "original name.json", additionalJson);
		
		finder.setFilePath("/static-root");
		finder.setFileNameSelector("name");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testPropertySelector() throws Exception {
		filesystem.put(null, "static-file.json", additionalJson);
		
		finder.setFilePath("static-file.json");
		finder.setPropertySelector("id");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional.get("123"), result);
	}
}
