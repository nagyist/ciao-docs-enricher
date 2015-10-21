ciao-docs-enricher
==================

*CIP to enrich parsed documents with additional properties*

Introduction
------------

The purpose of this CIP is to process an incoming [parsed document](https://github.com/nhs-ciao/ciao-docs-parser/ciao-docs-parser-model) by enriching it with additional properties before publishing the enriched document for further processing by other CIPs.

`ciao-docs-enricher` is built on top of [Apache Camel](http://camel.apache.org/) and [Spring Framework](http://projects.spring.io/spring-framework/), and can be run as a stand-alone Java application, or via [Docker](https://www.docker.com/).

Each application can host multiple [routes](http://camel.apache.org/routes.html), where each route follows the following basic structure:

>   input queue (JMS) -\> [DocumentEnricher](./ciao-docs-enricher/src/main/java/uk/nhs/ciao/docs/enricher/DocumentEnricher.java) or [AsyncDocumentEnricher](./ciao-docs-enricher/src/main/java/uk/nhs/ciao/docs/enricher/AsyncDocumentEnricher.java) -\> output queue (JMS)

-	*The input and output queues both use the JSON-encoded representation of [ParsedDocument](https://github.com/nhs-ciao/ciao-docs-parser/ciao-docs-parser-model/src/main/java/uk/nhs/ciao/docs/parser/ParsedDocument.java).*

The details of the JMS queues and document enrichers are specified at runtime through a combination of [ciao-configuration](https://github.com/nhs-ciao/ciao-utils) properties and Spring XML files.

**Provided document enricher implementations:**

-   [JsonResourceDocumentEnricher](./ciao-docs-enricher/src/main/java/uk/nhs/ciao/docs/enricher/JsonResourceDocumentEnricher.java) - An enricher which reads JSON content from the filesystem or classpath and merges it into the document properties. This can be used to include static content which cannot be obtained from the original source document. The resource should contain a JSON-encoded representation of a [ParsedDocument](https://github.com/nhs-ciao/ciao-docs-parser/ciao-docs-parser-model/src/main/java/uk/nhs/ciao/docs/parser/ParsedDocument.java).

***Planned future document enricher implementations:***

-   `PDSDocumentEnricher` - An enricher which performs a PDS lookup based on properties previously extracted from the source document (e.g. NHS number), and merges the retrieved detail into the document properties.

For more advanced usages, a custom document enricher can be integrated by implementing one of the enricher Java interfaces and providing a suitable spring XML configuration on the classpath.

Configuration
-------------

For further details of how ciao-configuration and Spring XML interact, please see [ciao-core](https://github.com/nhs-ciao/ciao-core).

### Spring XML

On application start-up, a series of Spring Framework XML files are used to construct the core application objects. The created objects include the main Camel context, input/output components, routes and any intermediate processors.

The configuration is split into multiple XML files, each covering a separate area of the application. These files are selectively included at runtime via CIAO properties, allowing alternative technologies and/or implementations to be chosen. Each imported XML file can support a different set of CIAO properties.

The Spring XML files are loaded from the classpath under the [META-INF/spring](./ciao-docs-enricher/src/main/resources/META-INF/spring) package.

**Core:**

-   `beans.xml` - The main configuration responsible for initialising properties, importing additional resources and starting Camel.

**Processors:**

-   `processors/default.xml` - Creates a single `JsonResourceDocumentEnricher` to load static content from the classpath or filesystem

**Messaging:**

-   `messaging/activemq.xml` - Configures ActiveMQ as the JMS implementation for input/output queues.
-   `messaging/activemq-embedded.xml` - Configures an internal embedded ActiveMQ as the JMS implementation for input/output queues. *(For use during development/testing)*

### CIAO Properties

At runtime ciao-docs-enricher uses the available CIAO properties to determine which Spring XML files to load, which Camel routes to create, and how individual routes and components should be wired.

**Camel Logging:**

-	`camel.log.mdc` - Enables/disables [Mapped Diagnostic Context](http://camel.apache.org/mdc-logging.html) in Camel. If enabled, additional Camel context properties will be made available to Log4J and Logstash. 
-	`camel.log.trace` - Enables/disables the [Tracer](http://camel.apache.org/tracer.html) interceptor for Camel routes.
-	`camel.log.debugStreams` - Enables/disables [debug logging of streaming messages](http://camel.apache.org/how-do-i-enable-streams-when-debug-logging-messages-in-camel.html) in Camel.

**Spring Configuration:**

-   `processorConfig` - Selects which processor configuration to load:
    `processors/${processorConfig}.xml`

-   `messagingConfig` - Selects which messaging configuration to load:
    `messaging/${messagingConfig}.xml`

**Routes:**

-   `documentEnricherRoutes` - A comma separated list of route names to build

The list of route names serves two purposes. Firstly it determines how many routes to build, and secondly each name is used as a prefix to specify the individual properties of that route.

**Route Configuration:**

>   For 'specific' properties unique to a single route, use the prefix:
>   `documentEnricherRoutes.${routeName}.`
>
>   For 'generic' properties covering all routes, use the prefix:
>   `documentEnricherRoutes.`

-   `inputQueue` - Selects which queue to consume incoming documents from
-   `enricherId` - The Spring ID of the enricher to use when enriching documents
-   `outputQueue` - Selects which queue to publish enriched documents to

**Default Processor​:**

>   These properties only apply when using: `processorConfiguration=default`

-   `staticJson.resourcePaths` - A comma separated list of JSON resources to include. Spring resource loader syntax is supported, e.g. `classpath:`, `file:` etc).

### Example
```INI
# Camel logging
camel.log.mdc=true
camel.log.trace=false
camel.log.debugStreams=false

# Select which processor config to use (via dynamic spring imports)
processorConfig=default

# Select which messaging config to use (via dynamic spring imports)
messagingConfig=activemq
# messagingConfig=activemq-embedded

# ActiveMQ settings (if messagingConfig=activemq)
activemq.brokerURL=tcp://localhost:61616
activemq.userName=smx
activemq.password=smx

# Setup route names (and how many routes to build)
documentEnricherRoutes=default

# Setup 'shared' properties across all-routes
documentEnricherRoutes.outputQueue=enriched-documents

# Setup per-route properties (can override the shared properties)
documentEnricherRoutes.default.enricherId=staticJson
documentEnricherRoutes.default.inputQueue=parsed-documents

staticJson.resourcePaths=classpath:/json/extra-detail.json
inProgressFolder=./in-progress
```

Building and Running
--------------------

To pull down the code, run:

	git clone https://github.com/nhs-ciao/ciao-docs-enricher.git
	
You can then compile the module via:

    cd ciao-docs-enricher-parent
	mvn clean install -P bin-archive

This will compile a number of related modules - the main CIP module is `ciao-docs-enricher`, and the full binary archive (with dependencies) can be found at `ciao-docs-enricher\target\ciao-docs-enricher-{version}-bin.zip`. To run the CIP, unpack this zip to a directory of your choosing and follow the instructions in the README.txt.

The CIP requires access to various file system directories and network ports (dependent on the selected configuration):

**etcd**:
 -  Connects to: `localhost:2379`

**ActiveMQ**:
 -  Connects to: `localhost:61616`

**Filesystem**:
 -  If etcd is not available, CIAO properties will be loaded from: `~/.ciao/`
 -  The default configuration will load JSON files for the filesystem if any `file://` URLs are specified in the `staticJson.resourcePaths` property. This can be altered by changing the CIAO properties configuration (via etcd, or the properties file in `~/.ciao/`)
 -  If an incoming document cannot be converted, the CIP will write an event to the folder specified by the `inProgressFolder` property.
