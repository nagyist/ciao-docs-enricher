package uk.nhs.ciao.docs.enricher;

import uk.nhs.ciao.camel.CamelApplication;
import uk.nhs.ciao.camel.CamelApplicationRunner;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

/**
 * The main ciao-docs-enricher application
 * <p>
 * The application configuration is handled by Spring loading META-INF/spring/beans.xml 
 * resource off the class-path. Additional spring configuration is loaded based on
 * properties specified in CIAO-config (ciao-docs-enricher.properties). At runtime the application
 * can start multiple routes (one per input folder) determined via the specified CIAO-config properties. 
 * <p>
 * The main flow of the application is:
 * <code>Input source (JMS queue) -> document enricher (property extraction)
 * -> Output sink (JMS queue)</code>
 * <p>
 * The following properties configure which additional spring configuration to load:
 * 
 * <dl>
 * 
 * <dt>processorConfig:</dt>
 * <dd><code>META-INF/spring/processors/${value}.xml</code></dd>
 * <dd>Configures which enrichers to load. These are later referenced by ID when
 * choosing routes via CIAO-properties</dd>
 * 
 * <dt>messagingConfig:</dt>
 * <dd><code>META-INF/spring/messaging/${value}.xml</code><dd>
 * <dd>Configures which Camel messaging component to use for publishing output messages. The component
 * should be mapped to the 'jms' ID.</dd>
 * </dl>
 */
public class DocumentEnricherApplication extends CamelApplication {
	/**
	 * Runs the document enricher application
	 * 
	 * @see CIAOConfig#CIAOConfig(String[], String, String, java.util.Properties)
	 * @see CamelApplicationRunner
	 */
	public static void main(final String[] args) throws Exception {
		final CamelApplication application = new DocumentEnricherApplication(args);
		CamelApplicationRunner.runApplication(application);
	}
	
	public DocumentEnricherApplication(final String... args) throws CIAOConfigurationException {
		super("ciao-docs-enricher.properties", args);
	}
	
	public DocumentEnricherApplication(final CIAOConfig ciaoConfig, final String... args) {
		super(ciaoConfig, args);
	}
}
