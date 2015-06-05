package uk.nhs.ciao.docs.enricher;


import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.CIPRoutes;
import uk.nhs.ciao.camel.CamelApplication;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

/**
 * Configures multiple camel document parser routes determined by properties specified
 * in the applications registered {@link CIAOConfig}.
 * <p>
 * The 'bootstrap' / {@link #ROOT_PROPERTY} determines which named routes to created (via
 * a comma-separated list).
 * <p>
 * The properties of each route are then looked up via the <code>${ROOT_PROPERTY}.${routeName}.${propertyName}</code>,
 * falling back to <code>${ROOT_PROPERTY}.${propertyName}</code> if a specified property is not provided.
 * This allows for shorthand specification of properties when they are shared across multiple routes.
 * <p>
 * The following properties are supported per named route:
 * <dl>
 * <dt>inputQueue<dt>
 * <dd>The name of the queue input messages should be read from</dd>
 * 
 * <dt>enricherId<dt>
 * <dd>The spring ID of this routes document enricher</dd>
 * 
 * <dt>outputQueue<dt>
 * <dd>The name of the queue output messages should be sent to</dd>
 */
public class DocumentEnricherRoutes extends CIPRoutes {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEnricherRoutes.class);
	
	/**
	 * The root property 
	 */
	public static final String ROOT_PROPERTY = "documentEnricherRoutes";
	
	/**
	 * Creates multiple document parser routes
	 * 
	 * @throws RuntimeException If required CIAO-config properties are missing
	 */
	@Override
	public void configure() {
		super.configure();
		
		final CIAOConfig config = CamelApplication.getConfig(getContext());
		
		try {
			final String[] routeNames = config.getConfigValue(ROOT_PROPERTY).split(",");
			for (final String routeName: routeNames) {
				final ParseDocumentRouteBuilder builder = new ParseDocumentRouteBuilder(
						routeName, config);
				builder.configure();
			}
		} catch (CIAOConfigurationException e) {
			throw new RuntimeException("Unable to build routes from CIAOConfig", e);
		}
	}
	
	/**
	 * Creates a Camel route for the specified name / property prefix.
	 * <p>
	 * Each configurable property is determined by:
	 * <ul>
	 * <li>Try the specific property: <code>${ROOT_PROPERTY}.${name}.${propertyName}</code></li>
	 * <li>If missing fallback to: <code>${ROOT_PROPERTY}.${propertyName}</code></li>
	 * </ul>
	 */
	private class ParseDocumentRouteBuilder {
		private final String name;
		private final String inputQueue;
		private final String enricherId;
		private final String outputQueue;
		
		/**
		 * Creates a new route builder for the specified name / property prefix
		 * 
		 * @param name The route name / property prefix
		 * @throws CIAOConfigurationException If required properties were missing
		 */
		public ParseDocumentRouteBuilder(final String name, final CIAOConfig config) throws CIAOConfigurationException {
			this.name = name;
			this.inputQueue = findProperty(config, "inputQueue");
			this.enricherId = findProperty(config, "enricherId");
			this.outputQueue = findProperty(config, "outputQueue");
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
		public void configure() {
			final Registry registry = getContext().getRegistry();
			final Object enricher = registry.lookupByName(enricherId);
			final DocumentEnricherProcessor processor = DocumentEnricherProcessor.createProcessor(enricher);
			
			from("jms:queue:" + inputQueue)
			.id("parse-document-" + name)
			.streamCaching()
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
}
