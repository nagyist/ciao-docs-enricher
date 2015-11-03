package uk.nhs.ciao.docs.enricher;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.parser.PropertySelector;
import uk.nhs.ciao.util.TreeMerge;

/**
 * Enriches documents dynamically based on content extracted from the existing
 * document properties.
 * <p>
 * {@link #enrichablePropertiesSelectors} selects which section or sections of the document
 * should be enriched. The selected object must be a dynamic map - if nothing is selected the
 * root document properties are used.
 * <p>
 * {@link #lookupKeySelectors} selects a set of key/value pairs from the document to use
 * as lookup keys for the dynamic data.
 * <p>
 * The selected lookup keys are sent to {@link #propertiesFinder}, and any returned properties
 * are added to the current document section.
 */
public class DynamicDocumentEnricher implements DocumentEnricher {
	private final TreeMerge treeMerge;	
	private final Set<PropertySelector> enrichablePropertiesSelectors;
	private final Set<PropertySelector> lookupKeySelectors;
	private DocumentPropertiesFinder propertiesFinder;
	
	public DynamicDocumentEnricher() {
		treeMerge = new TreeMerge();	
		enrichablePropertiesSelectors = Sets.newLinkedHashSet();
		lookupKeySelectors = Sets.newLinkedHashSet();
	}
	
	public DynamicDocumentEnricher(final DocumentPropertiesFinder propertiesFinder) {
		this();
		this.propertiesFinder = propertiesFinder;
	}
	
	public void setPropertiesFinder(final DocumentPropertiesFinder propertiesFinder) {
		this.propertiesFinder = propertiesFinder;
	}
	
	public void setEnrichablePropertiesSelectors(final Collection<String> enrichablePropertiesSelectors) {
		this.enrichablePropertiesSelectors.clear();
		for (final String selector: enrichablePropertiesSelectors) {
			this.enrichablePropertiesSelectors.add(PropertySelector.valueOf(selector));
		}
	}
	
	public void setLookupKeySelectors(final Collection<String> lookupKeySelectors) {
		this.lookupKeySelectors.clear();
		for (final String selector: lookupKeySelectors) {
			this.lookupKeySelectors.add(PropertySelector.valueOf(selector));
		}
	}
	
	/**
	 * Enriches the document by selecting the sections to enrich, extracting lookup keys and
	 * adding properties returned by the properties finder.
	 * 
	 * @throws Exception If {@link #propertiesFinder} fails when attempting to find properties
	 */
	@Override
	public ParsedDocument enrichDocument(final ParsedDocument document) throws Exception {
		if (document == null) {
			return null;
		}
		
		final Map<String, Object> lookupKeys = Maps.newLinkedHashMap();
		for (final Map<String, Object> properties: getEnrichableProperties(document)) {
			getLookupKeys(properties, lookupKeys);
			final Map<String, Object> additionalProperties = findAdditionalProperties(lookupKeys);
			addAdditionalProperties(additionalProperties, properties);
			
			lookupKeys.clear();
		}
		
		return document;
	}
	
	private List<Map<String, Object>> getEnrichableProperties(final ParsedDocument document) {
		final List<Map<String, Object>> enrichableProperties = Lists.newArrayList();
		
		if (enrichablePropertiesSelectors.isEmpty()) {
			enrichableProperties.add(document.getProperties());
		} else {
			for (final PropertySelector selector: enrichablePropertiesSelectors) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				final List<Map<String, Object>> result = (List)selector.selectAllValues(Map.class, document.getProperties());
				enrichableProperties.addAll(result);
			}
		}
		
		return enrichableProperties;
	}
	
	private void getLookupKeys(final Map<String, Object> properties, final  Map<String, Object> lookupKeys) {
		for (final PropertySelector selector: lookupKeySelectors) {
			lookupKeys.putAll(selector.selectAll(properties));
		}
	}
	
	private Map<String, Object> findAdditionalProperties(final Map<String, Object> lookupKeys) throws Exception {
		return propertiesFinder == null ? Collections.<String, Object>emptyMap() : propertiesFinder.findProperties(lookupKeys);
	}
	
	private void addAdditionalProperties(final Map<String, Object> additionalProperties, final Map<String, Object> properties) {
		if (additionalProperties != null && !additionalProperties.isEmpty()) {
			treeMerge.mergeInto(additionalProperties, properties);
		}
	}
}
