package uk.nhs.ciao.docs.enricher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

import uk.nhs.ciao.docs.parser.ParsedDocument;

/**
 * A document enricher which merges the incoming document with properties from
 * a static JSON resource.
 * <p>
 * The resource is loaded using the configured spring resource loader - therefore the standard
 * spring URLs are supported (e.g. classpath, file, etc)
 * 
 * @see ResourceLoader
 */
public class JsonResourceDocumentEnricher implements DocumentEnricher {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};
	
	private final String resourcePath;
	private final ObjectMapper objectMapper;
	private boolean failOnMissingResource = false;
	
	@Autowired
	private final ResourceLoader resourceLoader;
	
	/**
	 * Constructs a new enricher which will add properties from the specified path
	 * 
	 * @param resourcePath The path of the resource to add (using spring URLs)
	 */
	public JsonResourceDocumentEnricher(final String resourcePath) {
		this(resourcePath, new ObjectMapper());
	}
	
	/**
	 * Constructs a new enricher which will add properties from the specified path using the
	 * specified jackson object mapper
	 * 
	 * @param resourcePath The path of the resource to add (using spring URLs)
	 * @param objectMapper The jackson object mapper to use when unmarshalling the JSON resource
	 */
	public JsonResourceDocumentEnricher(final String resourcePath, final ObjectMapper objectMapper) {
		this.resourcePath = Preconditions.checkNotNull(resourcePath);
		this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
		this.resourceLoader = new DefaultResourceLoader();
	}
	
	/**
	 * Sets if the enrichment should fail if the resource is not found.
	 * <p>
	 * The default is to continue on missing resources
	 * 
	 * @param failOnMissingResource true if the enrichment should fail on missing resources
	 */
	public void setFailOnMissingResource(final boolean failOnMissingResource) {
		this.failOnMissingResource = failOnMissingResource;
	}
	
	/**
	 * Enriches the document by loading the JSON resource, then merging the content with 
	 * the incoming parsed document properties
	 */
	@Override
	public ParsedDocument enrichDocument(final ParsedDocument document) throws Exception {
		final Resource resource = resourceLoader.getResource(resourcePath);
		
		if (resource.exists()) {
			final Map<String, Object> additionalProperties = readJsonResource(resource);
			mergeProperties(document.getProperties(), additionalProperties);
		} else if (failOnMissingResource) {
			throw new Exception("Resource could not be loaded: " + resourcePath);
		}
		
		return document;
	}
	
	private void mergeProperties(final Map<String, Object> properties, final Map<String, Object> additionalProperties) {
		for (final Entry<String, Object> property: additionalProperties.entrySet()) {
			mergeProperty(properties, property.getKey(), property.getValue());
		}
	}
	
	@SuppressWarnings("unchecked")
	private void mergeProperty(final Map<String, Object> properties, final String name, final Object value) {
		final Object previousValue = properties.get(name);
		
		if (previousValue == null) {
			properties.put(name, value);
		} else if (previousValue instanceof Map && value instanceof Map) {
			final Map<String, Object> previousMap = (Map<String, Object>)previousValue;
			final Map<String, Object> additionalMap = (Map<String, Object>)value;
			mergeProperties(previousMap, additionalMap);
		} else if (previousValue instanceof List) {
			final List<Object> previousList = (List<Object>)previousValue;
			if (value instanceof List) {
				previousList.addAll((List<Object>)value);
			} else {
				previousList.add(value);
			}
		} // else - do not overwrite!
	}
	
	private Map<String, Object> readJsonResource(final Resource resource) throws IOException {
		InputStream in = null;
		try {
			in = resource.getInputStream();
			return objectMapper.readValue(in, MAP_TYPE);
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
