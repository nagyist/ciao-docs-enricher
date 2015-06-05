ciao-docs-enricher
==================

*CIP to enrich parsed documents with additional properties*

Introduction
------------

The purpose of this CIP is to process an incoming [parsed document][1] by
enriching it with additional properties before publishing the enriched document
for further processing by other CIPs.

[1]: <https://github.com/nhs-ciao/ciao-docs-parser>

`ciao-docs-enricher` is built on top of [Apache Camel][2] and [Spring
Framework][3], and can be run as a stand-alone Java application, or via
[Docker][4].

[2]: <http://camel.apache.org/>

[3]: <http://projects.spring.io/spring-framework/>

[4]: <https://www.docker.com/>

Each application can host multiple [routes][5], where each route follows the
following basic structure:

[5]: <http://camel.apache.org/routes.html>

>   input JMS queue -\> `DocumentEnricher` or `AsyncDocumentEnricher` -\> output
>   JMS queue

The details of the JMS queues and document enrichers are specified at runtime
through a combination of [ciao-configuration][6] properties and Spring XML
files.

[6]: <https://github.com/nhs-ciao/ciao-utils>

The following document enricher implementations are provided:

-   [JsonResourceDocumentEnricher][7] - An enricher which reads JSON content
    from the filesystem or classpath and merges it into the document properties.
    This can be used to include static content which cannot be obtained from the
    original source document.

[7]: <./ciao-docs-enricher/src/main/java/uk/nhs/ciao/docs/enricher/JsonResourceDocumentEnricher.java>

-   `PDSDocumentEnricher` - An enricher which performs a PDS lookup based on
    properties previously extracted from the source document (e.g. NHS number),
    and merges the retrieved detail into the document properties.

For more advanced usages, a custom document enricher can be integrated by
implementing one of the enricher Java interfaces and providing a suitable spring
XML configuration on the classpath.

Configuration
-------------

For further details of how ciao-configuration and Spring XML interact, please
see [ciao-core][8].

[8]: <https://github.com/nhs-ciao/ciao-core>

### Spring XML

On application start-up, a series of Spring Framework XML files are used to
construct the core application objects. The created objects include the main
Camel context, input/output components, routes and any intermediate processors.

The configuration is split into multiple XML files, each covering a separate
area of the application. These files are selectively included at runtime via
CIAO properties, allowing alternative technologies and/or implementations to be
chosen. Each imported XML file can support a different set of CIAO properties.

The Spring XML files are loaded from the classpath under the
[META-INF/spring][9] package.

[9]: <./ciao-docs-enricher/src/main/resources/META-INF/spring>

**Core:**

-   `beans.xml` - The main configuration responsible for initialising
    properties, importing additional resources and starting Camel.

**Processors:**

-   `processors/default.xml` - Creates a single `JsonResourceDocumentEnricher`
    to load static content from the classpath or filesystem

**Messaging:**

-   `messaging/activemq.xm`l - Configures ActiveMQ as the JMS implementation for
    input/output queues.

-   `messaging/activemq-embedded.xml`  - Configures an internal embedded
    ActiveMQ as the JMS implementation for input/output queues. *(For use during
    development/testing)*

### CIAO Properties
