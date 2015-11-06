package uk.nhs.ciao.docs.enricher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import uk.nhs.ciao.docs.parser.PropertySelector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Closeables;

/**
 * {@link DocumentPropertiesFinder} which finds properties from JSON resources.
 * <p>
 * The JSON resource to load is determined by:
 * <ul>
 * <li>{@link #resourceNameSelector} - optional selector for using a dynamic file name based on the lookup keys.
 * <li>{@link #resourceSuffix} - optional suffix for adding to dynamic file names (default is .json)
 * <li>{@link #resourcePath} - optional parent folder used for dynamic file names, or the complete file name/path (if no selector)
 * </ul>
 * <p>
 * At least one of <code>resourceNameSelector</code> and <code>resourcePath</code> needs to be specified.
 * <p>
 * If {@link #propertySelector} is specified, it is used to select a section of the JSON resource to return (again using
 * incoming lookup keys). Otherwise the entire JSON structure is returned.
 */
public class JsonResourceDocumentPropertiesFinder implements DocumentPropertiesFinder {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
		// Jackson type reference - required to ensure generic type is available via reflection
	};
	
	private final ResourceLoader resourceLoader;
	private final ObjectMapper objectMapper;
	private String resourcePath;
	private PropertySelector resourceNameSelector;
	private String resourceSuffix = ".json";
	private PropertySelector propertySelector;
	
	@Autowired
	public JsonResourceDocumentPropertiesFinder(final ResourceLoader resourceLoader) {
		this(resourceLoader, new ObjectMapper());
	}
	
	public JsonResourceDocumentPropertiesFinder(final ResourceLoader resourceLoader, final ObjectMapper objectMapper) {
		this.resourceLoader = Preconditions.checkNotNull(resourceLoader);
		this.objectMapper = Preconditions.checkNotNull(objectMapper);
	}
	
	public void setResourcePath(final String resourcePath) {
		this.resourcePath = Strings.emptyToNull(resourcePath);
	}
	
	public void setResourceNameSelector(final String resourceNameSelector) {
		this.resourceNameSelector = Strings.isNullOrEmpty(resourceNameSelector) ? null :
			PropertySelector.valueOf(resourceNameSelector);
	}
	
	public void setResourceSuffix(final String resourceSuffix) {
		this.resourceSuffix = Strings.emptyToNull(resourceSuffix);
	}
	
	public void setPropertySelector(final String propertySelector) {
		this.propertySelector = Strings.isNullOrEmpty(propertySelector) ? null :
			PropertySelector.valueOf(propertySelector);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> findProperties(final Map<String, Object> lookupKeys) throws Exception {
		final InputStream inputStream = getJson(lookupKeys);
		try {
			if (inputStream == null) {
				return Collections.emptyMap();
			}
			
			final Map<String, Object> properties = objectMapper.readValue(inputStream, MAP_TYPE);
			if (propertySelector == null) {
				return properties;
			}
			
			final Object value = propertySelector.selectValue(lookupKeys);
			if (value == null) {
				return Collections.emptyMap();
			}
			
			return PropertySelector.valueOf(value.toString()).selectValue(Map.class, properties);
		} finally {
			Closeables.closeQuietly(inputStream);
		}
	}
	
	private InputStream getJson(final Map<String, Object> lookupKeys) throws IOException {
		String parent = null;
		String resourceName = Strings.isNullOrEmpty(resourcePath) ? null : resourcePath;
		
		if (resourceNameSelector != null) {
			final Object value = resourceNameSelector.selectValue(lookupKeys);
			if (value != null) {
				parent = resourceName;
				resourceName = value.toString();
				if (resourceSuffix != null) {
					resourceName += resourceSuffix;
				}
			}
		}

		return getJson(parent, resourceName);
	}
	
	private InputStream getJson(final String parent, final String resourceName) throws IOException {
		final StringBuilder path = new StringBuilder();
		if (!Strings.isNullOrEmpty(parent)) {
			path.append(parent);
			if (!parent.endsWith("/")) {
				path.append('/');
			}
		}
		path.append(resourceName);

		final Resource resource = resourceLoader.getResource(path.toString());		
		return resource.exists() ? resource.getInputStream() : null;
	}
}
