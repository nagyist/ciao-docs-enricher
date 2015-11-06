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

/**
 * {@link DocumentPropertiesFinder} which finds properties from a database.
 * <p>
 * The Camel JDBC component is used (via a {@link ProducerTemplate}) to query the database:
 * <ul>
 * <li>{@link #dataSourceId} - The Camel registry id of the {@link javax.sql.DataSource} to use
 * <li>{@link #sqlQuery} - The select query used to find the properties. A single named parameter
 * 		(of the form <code>:?id</code>) should form part of the WHERE clause.
 * <li>{@link #idParameter} - The name of the SQL parameter included in the WHERE clause
 * <li>{@link #idSelector} - The selector for finding ID values from the incoming lookup keys - the resulting
 * 		value forms the dynamic part of the SQL WHERE clause
 * <li>{@link #objectMapper} - Optional mapper used for converting embedded JSON values
 * <li>{@link #jsonColumn} - Optional name of a returned column containing data as an embedded JSON string
 * </ul>
 * <p>
 * As a minimum {@link #dataSourceId}, {@link #sqlQuery}, {@link #idParameter} and {@link #idSelector} should be specified.
 * <p>
 * The first row returned by the query is converted to a properties map:
 * <li>In the standard mode, the result set column names are used as the property map keys. 'As' aliases
 * in the sql query can be used to customise the properties.
 * <li>Alternatively if {@link #jsonColumn} is specified, the string value associated with this column is
 * extracted from the result set and converted from JSON into the properties map.
 * <p>
 * The finder will return an empty map if the sql query returns no rows, but throws an exception if either
 * the database query or the JSON conversion fails.
 */
public class JdbcDocumentPropertiesFinder implements DocumentPropertiesFinder {
	private static final Map<String, Object> EMPTY_PROPERTIES = Collections.emptyMap();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
		// Jackson type reference - required to ensure generic type is available via reflection
	};
	
	private final ProducerTemplate producerTemplate;
	private final ObjectMapper objectMapper;
	private String dataSourceId;
	private String sqlQuery;
	private String idParameter;
	private PropertySelector idSelector;
	private String jsonColumn;

	/**
	 * Constructs a new finder backed by the specified Camel producer template
	 * <p>
	 * Spring auto-wiring is supported
	 */
	@Autowired
	public JdbcDocumentPropertiesFinder(final ProducerTemplate producerTemplate) {
		this(producerTemplate, new ObjectMapper());
	}
	
	/**
	 * Constructs a new finder backed by the specified Camel producer template
	 * and Jackson object mapper
	 * <p>
	 * Spring auto-wiring is supported
	 */
	public JdbcDocumentPropertiesFinder(final ProducerTemplate producerTemplate, final ObjectMapper objectMapper) {
		this.producerTemplate = Preconditions.checkNotNull(producerTemplate);
		this.objectMapper = objectMapper;
	}
	
	/**
	 * The ID of the {@link javax.sql.DataSource} in the Camel registry
	 */
	public void setDataSourceId(final String dataSourceId) {
		this.dataSourceId = Strings.emptyToNull(dataSourceId);
	}
	
	/**
	 * The select query to use when finding data.
	 * <p>
	 * A single named parameter (called {@link #idParameter}) is required
	 * as part of the WHERE clause. The property value returned by {@link #idSelector} 
	 * is bound as the value.
	 * <p>
	 * The format of the named parameter is <code>:?id</code>
	 */
	public void setSqlQuery(final String sqlQuery) {
		this.sqlQuery = Strings.emptyToNull(sqlQuery);
	}
	
	/**
	 * The name of the ID parameter included in {@link #sqlQuery}
	 */
	public void setIdParameter(final String idParameter) {
		this.idParameter = Strings.emptyToNull(idParameter);
	}
	
	/**
	 * Selector for finding the ID value from the incoming lookup keys
	 */
	public void setIdSelector(final String idSelector) {
		this.idSelector = idSelector == null ? null : PropertySelector.valueOf(idSelector);
	}
	
	/**
	 * Optional value defining which column (if any) contains the embedded
	 * JSON properties strings
	 */
	public void setJsonColumn(final String jsonColumn) {
		this.jsonColumn = Strings.emptyToNull(jsonColumn);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * If the database query executes without error but returns no data and empty properties
	 * map is returned.
	 * 
	 * @throws Exception If the database query fails or if the optional embedded JSON value is not valid
	 */
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

	/**
	 * Queries the database via a camel producer template exchange
	 * <p>
	 * The Camel JDBC component is used - the default outputType of 'SelectList' is used.
	 * This will return all rows from the query in memory - however queries should
	 * typically return zero or one rows.
	 * 
	 * @throws Exception If an exception was thrown during the JDBC exchange
	 */
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
