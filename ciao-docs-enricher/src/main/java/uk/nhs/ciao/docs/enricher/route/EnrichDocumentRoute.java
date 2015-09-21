package uk.nhs.ciao.docs.enricher.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.spi.TransactionErrorHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.camel.BaseRouteBuilder;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.docs.enricher.DocumentEnricherProcessor;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

/**
 * Creates a Camel route for the specified name / property prefix.
 * <p>
 * Each configurable property is determined by:
 * <ul>
 * <li>Try the specific property: <code>${ROOT_PROPERTY}.${name}.${propertyName}</code></li>
 * <li>If missing fallback to: <code>${ROOT_PROPERTY}.${propertyName}</code></li>
 * </ul>
 */
public class EnrichDocumentRoute extends BaseRouteBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(EnrichDocumentRoute.class);
	
	/**
	 * The root property 
	 */
	public static final String ROOT_PROPERTY = "documentEnricherRoutes";
	
	private final String name;
	private final String inputQueue;
	private final String enricherId;
	private final String outputQueue;
	private String inProgressFolderManagerUri;
	
	/**
	 * Creates a new route builder for the specified name / property prefix
	 * 
	 * @param name The route name / property prefix
	 * @throws CIAOConfigurationException If required properties were missing
	 */
	public EnrichDocumentRoute(final String name, final CIAOConfig config) throws CIAOConfigurationException {
		this.name = name;
		this.inputQueue = findProperty(config, "inputQueue");
		this.enricherId = findProperty(config, "enricherId");
		this.outputQueue = findProperty(config, "outputQueue");
	}
	
	public void setInProgressFolderManagerUri(final String inProgressFolderManagerUri) {
		this.inProgressFolderManagerUri = inProgressFolderManagerUri;
	}
	
	/**
	 * Try the specific 'named' property then fall back to the general 'all-routes' property
	 */
	private String findProperty(final CIAOConfig config, final String propertyName) throws CIAOConfigurationException {
		final String specificName = ROOT_PROPERTY + "." + name + "." + propertyName;
		final String genericName = ROOT_PROPERTY + "." + propertyName;
		if (config.getConfigKeys().contains(specificName)) {
			return config.getConfigValue(specificName);
		} else if (config.getConfigKeys().contains(genericName)) {
			
			return config.getConfigValue(genericName);
		} else {
			throw new CIAOConfigurationException("Could not find property " + propertyName +
					" for route " + name);
		}
	}

	/**
	 * Configures / creates a new Camel route corresponding to the set of CIAO-config
	 * properties associated with the route name.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void configure() throws Exception {
		final Registry registry = getContext().getRegistry();
		final Object enricher = registry.lookupByName(enricherId);
		final DocumentEnricherProcessor processor = DocumentEnricherProcessor.createProcessor(enricher);
		
		from("jms:queue:" + inputQueue)
		.id("parse-document-" + name)
		.streamCaching()
		.errorHandler(new TransactionErrorHandlerBuilder()
				.maximumRedeliveries(0)) // redeliveries are disabled (enrichment is only tried once)
		.transacted("PROPAGATION_NOT_SUPPORTED")
		.doTry()
			.unmarshal().json(JsonLibrary.Jackson, ParsedDocument.class)
			.log(LoggingLevel.INFO, LOGGER, "Unmarshalled incoming JSON document")
			.process(processor)					
			.marshal().json(JsonLibrary.Jackson)
			.to("jms:queue:" + outputQueue)
		.doCatch(Exception.class)
			.log(LoggingLevel.ERROR, LOGGER, "Exception while enriching document: ${file:name}")
			.to("log:" + LOGGER.getName() + "?level=ERROR&showCaughtException=true")
			.handled(false);
	}
}
