package uk.nhs.ciao.docs.enricher;


import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;

import uk.nhs.ciao.camel.CamelApplication;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.docs.enricher.route.EnrichDocumentRoute;
import uk.nhs.ciao.docs.parser.route.InProgressFolderManagerRoute;

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
public class DocumentEnricherRoutes implements RoutesBuilder {	
	/**
	 * Creates multiple document parser routes
	 * 
	 * @throws Exception If required CIAO-config properties are missing
	 */
	@Override
	public void addRoutesToCamelContext(final CamelContext context) throws Exception {
		addEnrichDocumentRoutes(context);
		
		// services
		addInProgressFolderManagerRoute(context);
	}
	
	private void addEnrichDocumentRoutes(final CamelContext context) throws Exception {
		final CIAOConfig config = CamelApplication.getConfig(context);
		
		final String[] routeNames = config.getConfigValue(EnrichDocumentRoute.ROOT_PROPERTY).split(",");
		for (final String routeName: routeNames) {
			final EnrichDocumentRoute route = new EnrichDocumentRoute(
					routeName, config);
			route.setInProgressFolderManagerUri("direct:in-progress-folder-manager");
			
			context.addRoutes(route);
		}
	}
	
	private void addInProgressFolderManagerRoute(final CamelContext context) throws Exception {
		final InProgressFolderManagerRoute route = new InProgressFolderManagerRoute();
		
		route.setInProgressFolderManagerUri("direct:in-progress-folder-manager");
		route.setInternalRoutePrefix("in-progress-folder-manager");
		route.setInProgressFolderRootUri("file:{{inProgressFolder}}");
		
		context.addRoutes(route);
	}
}
