package uk.nhs.ciao.docs.enricher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.util.TreeMerge;

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
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
		// Jackson type reference - required to ensure generic type is available via reflection
	};
	
	private final List<String> resourcePaths;
	private final ObjectMapper objectMapper;
	private final TreeMerge treeMerge;
	private boolean failOnMissingResource = false;
	
	/**
	 * Loads the specified JSON resources
	 * <p>
	 * A default is provided on construction - however the @Autowired annotation allows
	 * spring to inject a suitable replacement at runtime (typically the main application
	 * context).
	 */
	@Autowired
	private final ResourceLoader resourceLoader;
	
	/**
	 * Constructs a new enricher which will add properties from the specified path
	 * 
	 * @param resourcePaths The paths of the resource to add (using spring URLs)
	 */
	public JsonResourceDocumentEnricher(final String... resourcePaths) {
		this(new ObjectMapper(), resourcePaths);
	}
	
	/**
	 * Constructs a new enricher which will add properties from the specified path using the
	 * specified jackson object mapper
	 * 
	 * @param resourcePaths The paths of the resources to add (using spring URLs)
	 * @param objectMapper The jackson object mapper to use when unmarshalling the JSON resource
	 */
	public JsonResourceDocumentEnricher(final ObjectMapper objectMapper, final String... resourcePaths) {
		this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
		this.resourcePaths = Lists.newArrayList(resourcePaths);
		this.treeMerge = new TreeMerge();
		this.resourceLoader = new DefaultResourceLoader();
		
		// Remove any null/empty paths
		final Iterator<String> iterator = this.resourcePaths.iterator();
		while (iterator.hasNext()) {
			if (Strings.isNullOrEmpty(iterator.next())) {
				iterator.remove();
			}
		}
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
		final Map<String, Object> properties = document.getProperties();

		for (final String resourcePath: resourcePaths) {
			final Resource resource = resourceLoader.getResource(resourcePath);
			
			if (resource.exists()) {
				final Map<String, Object> additionalProperties = readJsonResource(resource);
				treeMerge.mergeInto(additionalProperties, properties);
			} else if (failOnMissingResource) {
				throw new Exception("Resource could not be loaded: " + resourcePath);
			}
		}
		
		return document;
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
