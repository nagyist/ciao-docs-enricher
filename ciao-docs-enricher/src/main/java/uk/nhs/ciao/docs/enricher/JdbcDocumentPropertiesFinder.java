package uk.nhs.ciao.docs.enricher;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import uk.nhs.ciao.docs.parser.PropertySelector;

public class JdbcDocumentPropertiesFinder implements DocumentPropertiesFinder {
	private static final Map<String, Object> EMPTY_PROPERTIES = Collections.emptyMap();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
		// Jackson type reference - required to ensure generic type is available via reflection
	};
	
	private final ProducerTemplate producerTemplate;
	private String dataSourceId;
	private String sqlQuery;
	private String idParameter;
	private PropertySelector idSelector;
	private ObjectMapper objectMapper;
	private String jsonColumn;

	@Autowired
	public JdbcDocumentPropertiesFinder(final ProducerTemplate producerTemplate) {
		this.producerTemplate = Preconditions.checkNotNull(producerTemplate);
	}
	
	public void setDataSourceId(final String dataSourceId) {
		this.dataSourceId = Strings.emptyToNull(dataSourceId);
	}
	
	public void setSqlQuery(final String sqlQuery) {
		this.sqlQuery = Strings.emptyToNull(sqlQuery);
	}
	
	public void setIdParameter(final String idParameter) {
		this.idParameter = Strings.emptyToNull(idParameter);
	}
	
	public void setIdSelector(final String idSelector) {
		this.idSelector = idSelector == null ? null : PropertySelector.valueOf(idSelector);
	}
	
	public void setObjectMapper(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public void setJsonColumn(final String jsonColumn) {
		this.jsonColumn = Strings.emptyToNull(jsonColumn);
	}
	
	@Override
	public Map<String, Object> findProperties(final Map<String, Object> lookupKeys) throws Exception {
		Map<String, Object> properties = EMPTY_PROPERTIES;
		
		final Object id = idSelector.selectValue(lookupKeys);
		if (id != null) {
			properties = queryDatabase(id);
			
			if (jsonColumn != null) {
				Object value = properties.get(jsonColumn);
				if (value != null) {
					properties = objectMapper.readValue(value.toString(), MAP_TYPE);
				}
			}
		}
		
		return properties;
	}

	private Map<String, Object> queryDatabase(final Object id) throws Exception {
		final Exchange exchange = new DefaultExchange(producerTemplate.getCamelContext());
		exchange.setPattern(ExchangePattern.InOut);
		exchange.getIn().setHeader(idParameter, id);
		exchange.getIn().setBody(sqlQuery);
		
		producerTemplate.send("jdbc:" + dataSourceId + "?useHeadersAsParameters=true", exchange);
		if (exchange.getException() != null) {
			throw new IOException("Unable to lookup document properties for id: " + id,
					exchange.getException());
		}
		
		final Message message = exchange.hasOut() ? exchange.getOut() : exchange.getIn();
		if (message.getHeader("CamelJdbcRowCount", 0, Integer.class) < 1) {
			// no matching rows were found
			return EMPTY_PROPERTIES;
		}
		
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> results = message.getBody(List.class);
		if (results == null || results.isEmpty()) {
			// sanity check
			return EMPTY_PROPERTIES;
		}
		
		return results.get(0);
	}
}
