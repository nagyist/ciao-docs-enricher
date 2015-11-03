package uk.nhs.ciao.docs.enricher;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import uk.nhs.ciao.docs.parser.PropertySelector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * {@link DocumentPropertiesFinder} which finds properties from JSON files.
 * <p>
 * The JSON file to load is determined by:
 * <ul>
 * <li>{@link #fileNameSelector} - optional selector for using a dynamic file name based on the lookup keys.
 * <li>{@link #fileSuffix} - optional suffix for adding to dynamic file names (default is .json)
 * <li>{@link #filePath} - optional parent folder used for dynamic file names, or the complete file name/path (if no selector)
 * </ul>
 * <p>
 * At least one of <code>fileNameSelector</code> and <code>filePath</code> needs to be specified.
 * <p>
 * If {@link #propertySelector} is specified, it is used to select a section of the JSON file to return (again using
 * incoming lookup keys). Otherwise the entire JSON structure is returned.
 */
public class FileDocumentPropertiesFinder implements DocumentPropertiesFinder {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
		// Jackson type reference - required to ensure generic type is available via reflection
	};
	
	private final ObjectMapper objectMapper;
	private String filePath;
	private PropertySelector fileNameSelector;
	private String fileSuffix = ".json";
	private PropertySelector propertySelector;
	
	public FileDocumentPropertiesFinder(final ObjectMapper objectMapper) {
		this.objectMapper = Preconditions.checkNotNull(objectMapper);
	}
	
	public void setFilePath(final String filePath) {
		this.filePath = Strings.emptyToNull(filePath);
	}
	
	public void setFileNameSelector(final String fileNameSelector) {
		this.fileNameSelector = Strings.isNullOrEmpty(fileNameSelector) ? null :
			PropertySelector.valueOf(fileNameSelector);
	}
	
	public void setFileSuffix(final String fileSuffix) {
		this.fileSuffix = Strings.emptyToNull(fileSuffix);
	}
	
	public void setPropertySelector(final String propertySelector) {
		this.propertySelector = Strings.isNullOrEmpty(propertySelector) ? null :
			PropertySelector.valueOf(propertySelector);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> findProperties(final Map<String, Object> lookupKeys) throws Exception {
		final File file = getFile(lookupKeys);
		if (file == null || !file.isFile()) {
			return Collections.emptyMap();
		}
		
		final Map<String, Object> properties = objectMapper.readValue(file, MAP_TYPE);
		if (propertySelector == null) {
			return properties;
		}
		
		return propertySelector.selectValue(Map.class, properties);
	}
	
	private File getFile(final Map<String, Object> lookupKeys) {
		File file = null;
		
		if (!Strings.isNullOrEmpty(filePath)) {
			file = new File(filePath);
		}
		
		if (fileNameSelector != null) {
			final Object value = fileNameSelector.selectValue(lookupKeys);
			if (value != null) {
				String fileName = value.toString();
				if (fileSuffix != null) {
					fileName += fileSuffix;
				}
				
				if (file == null) {
					file = new File(fileName);
				} else {
					file = new File(file, fileName);
				}
			}
		}

		return file;
	}
}
