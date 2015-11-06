package uk.nhs.ciao.docs.enricher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

/**
 * Unit tests for {@link JsonResourceDocumentPropertiesFinder}
 */
public class JsonResourceDocumentPropertiesFinderTest {
	private ObjectMapper objectMapper;
	private ResourceLoader resourceLoader;
	private JsonResourceDocumentPropertiesFinder finder;
	private Map<String, Object> properties;
	private Map<String, Object> additional;
	private String additionalJson;
	
	@Before
	public void setup() throws Exception {
		objectMapper = new ObjectMapper();
		resourceLoader = Mockito.mock(ResourceLoader.class);
		finder = new JsonResourceDocumentPropertiesFinder(resourceLoader, objectMapper);
		
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
	
	private void addResource(final String name, final String value) throws IOException {
		final Resource resource = Mockito.mock(Resource.class);
		Mockito.when(resource.getInputStream()).thenAnswer(new Answer<InputStream>() {
			@Override
			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				return new ByteArrayInputStream(value.getBytes());
			}
		});
		Mockito.when(resource.exists()).thenReturn(true);
		Mockito.when(resourceLoader.getResource(name)).thenReturn(resource);
	}
	
	@Test
	public void testStaticResourceName() throws Exception {
		addResource("static-file.json", additionalJson);
		
		finder.setResourcePath("static-file.json");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testDynamicResourceName() throws Exception {
		addResource("original name.json", additionalJson);
		
		finder.setResourceNameSelector("name");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testResourcePathWithDynamicResourceName() throws Exception {
		addResource("/static-root/original name.json", additionalJson);
		
		finder.setResourcePath("/static-root");
		finder.setResourceNameSelector("name");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional, result);
	}
	
	@Test
	public void testPropertySelector() throws Exception {
		addResource("static-file.json", additionalJson);
		
		finder.setResourcePath("static-file.json");
		finder.setPropertySelector("id");
		final Map<String, Object> result = finder.findProperties(properties);
		Assert.assertEquals(additional.get("123"), result);
	}
}
