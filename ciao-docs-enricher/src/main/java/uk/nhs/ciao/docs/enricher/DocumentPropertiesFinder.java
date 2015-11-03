package uk.nhs.ciao.docs.enricher;

import java.util.Map;

/**
 * Finds dynamic document properties from a set of lookup key/value pairs
 */
public interface DocumentPropertiesFinder {
	/**
	 * Finds dynamic document properties from a set of lookup key/value pairs
	 * 
	 * @param lookupKeys Key/value pairs identifying the properties to find
	 * @return The properties associated with the <code>lookupKeys</code>
	 */
	Map<String, Object> findProperties(Map<String, Object> lookupKeys) throws Exception;
}