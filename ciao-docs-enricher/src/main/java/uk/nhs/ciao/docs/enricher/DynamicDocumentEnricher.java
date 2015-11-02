package uk.nhs.ciao.docs.enricher;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.parser.PropertySelector;
import uk.nhs.ciao.util.TreeMerge;

public class DynamicDocumentEnricher implements DocumentEnricher {
	private final TreeMerge treeMerge = new TreeMerge();	
	private final Set<PropertySelector> enrichablePropertiesSelectors = Sets.newLinkedHashSet();
	
	@Override
	public ParsedDocument enrichDocument(final ParsedDocument document) throws Exception {
		if (document == null) {
			return null;
		}
		
		for (final Map<String, Object> properties: getEnrichableProperties(document)) {
			final Map<String, Object> lookupKeys = getLookupKeys(properties);
			final Map<String, Object> additionalProperties = lookupAdditionalProperties(lookupKeys);
			addAdditionalProperties(properties, additionalProperties);
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
	
	private Map<String, Object> getLookupKeys(final Map<String, Object> properties) {
		throw new UnsupportedOperationException();
	}
	
	private Map<String, Object> lookupAdditionalProperties(final Map<String, Object> lookupKeys) throws Exception {
		throw new UnsupportedOperationException();
	}
	
	private void addAdditionalProperties(final Map<String, Object> properties, final Map<String, Object> additionalProperties) {
		if (additionalProperties != null && !additionalProperties.isEmpty()) {
			treeMerge.mergeInto(additionalProperties, properties);
		}
	}
}
