package uk.nhs.ciao.docs.enricher;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import uk.nhs.ciao.camel.CamelUtils;

/**
 * Unit tests for {@link JdbcDocumentPropertiesFinder}
 */
public class JdbcDocumentPropertiesFinderTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDocumentPropertiesFinderTest.class);
	private static DriverManagerDataSource DATA_SOURCE;
	
	@BeforeClass
	public static void createDatabase() throws Exception {
		System.setProperty("derby.system.home", "./target/derby-db");
		
		DATA_SOURCE = new DriverManagerDataSource();
		DATA_SOURCE.setUrl("jdbc:derby:memory:prop_finder;create=true");
		DATA_SOURCE.setUsername("PROP_FINDER");
		DATA_SOURCE.setPassword("PASS");
		
		insertDBFixtures();
	}
	
	/**
	 * Database tables as created from a script and test data is loaded
	 */
	private static void insertDBFixtures() throws Exception {
		final SimpleRegistry registry = new SimpleRegistry();
		registry.put("dataSource", DATA_SOURCE);
		
		final CamelContext context = new DefaultCamelContext(registry);
		ProducerTemplate producerTemplate = null;
		try {
			context.addRoutes(new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					from("direct:load-db-resource")
						.split(body(String.class).tokenize(";"))
						.pipeline()
							.convertBodyTo(String.class)
							.filter().simple("${body.trim.isEmpty} == false")
								.to("jdbc:dataSource")
							.end()
						.end()
					.end();
				}
			});
			
			context.start();
			
			producerTemplate = new DefaultProducerTemplate(context);
			producerTemplate.start();
			
			producerTemplate.sendBody("direct:load-db-resource", JdbcDocumentPropertiesFinder.class
					.getResourceAsStream("jdbc-finder-database.sql"));
		} finally {
			CamelUtils.stopQuietly(producerTemplate, context);
		}
	}
	
	@AfterClass
	public static void destroyDatabase() {
		try {
			DriverManager.getConnection("jdbc:derby:;drop=true;shutdown=true");
		} catch (SQLException e) {
			LOGGER.debug("Derby is expected to throw an exception on shutdown!", e);
		}
	}
	
	private CamelContext context;
	private ProducerTemplate producerTemplate;
	private JdbcDocumentPropertiesFinder finder;
	
	@Before
	public void setup() throws Exception {
		final SimpleRegistry registry = new SimpleRegistry();
		registry.put("dataSource", DATA_SOURCE);
		
		context = new DefaultCamelContext(registry);
		producerTemplate = new DefaultProducerTemplate(context);
		context.start();
		producerTemplate.start();
				
		finder = new JdbcDocumentPropertiesFinder(producerTemplate);
		finder.setDataSourceId("dataSource");
		finder.setObjectMapper(new ObjectMapper());
	}
	
	@After
	public void tearDown() {
		CamelUtils.stopQuietly(producerTemplate, context);
	}
	
	@Test
	public void testQueryColumnsAreMapped() throws Exception {
		finder.setIdSelector("personId");
		finder.setSqlQuery("SELECT ID as \"id\", NAME as \"name\" FROM NAMES WHERE id = :?pid");
		finder.setIdParameter("pid");
		
		final Map<String, Object> properties = finder.findProperties(createKeys(2));
		
		final Map<String, Object> expected = Maps.newHashMap();
		expected.put("id", "2");
		expected.put("name", "Mary Jones");
		Assert.assertEquals(expected, properties);
	}
	
	@Test
	public void testEmbeddedJsonIsMapped() throws Exception {
		finder.setIdSelector("personId");
		finder.setSqlQuery("SELECT * FROM JSON WHERE id = :?pid");
		finder.setIdParameter("pid");
		finder.setJsonColumn("JSON");
		
		final Map<String, Object> properties = finder.findProperties(createKeys(3));
		
		final Map<String, Object> expected = Maps.newHashMap();
		expected.put("firstName", "Peter");
		expected.put("secondName", "Davies");
		Assert.assertEquals(expected, properties);
	}
	
	@Test
	public void testNoMatchingRow() throws Exception {
		finder.setIdSelector("documentName");
		finder.setSqlQuery("SELECT * FROM NAMES WHERE id = :?pid");
		finder.setIdParameter("pid");
		
		final Map<String, Object> properties = finder.findProperties(createKeys(2));
		Assert.assertTrue(properties.isEmpty());
	}
	
	@Test(expected=Exception.class)
	public void testSqlExceptionsArePropegated() throws Exception {
		finder.setIdSelector("documentName");
		finder.setSqlQuery("SELECT * FROM INVALID_TABLE WHERE id = :?pid");
		finder.setIdParameter("pid");
		
		finder.findProperties(createKeys(2));
	}
	
	@Test(expected=Exception.class)
	public void testJsonExceptionsArePropegated() throws Exception {
		finder.setIdSelector("personId");
		finder.setSqlQuery("SELECT * FROM JSON WHERE id = :?pid");
		finder.setIdParameter("pid");
		finder.setJsonColumn("JSON");
		
		finder.findProperties(createKeys(1)); // JSON for id=1 has an invalid format
	}
	
	private Map<String, Object> createKeys(final Object id) {
		final Map<String, Object> lookupKeys = Maps.newHashMap();
		
		lookupKeys.put("personId", String.valueOf(id));
		lookupKeys.put("documentName", "another value");
		
		return lookupKeys;
	}
}
