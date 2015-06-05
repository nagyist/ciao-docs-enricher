ciao-docs-enricher
==================

*CIP to enrich parsed documents with additional properties*

Introduction
------------

The purpose of this CIP is to process an incoming [parsed document][1] by
enriching it with additional properties before publishing the enriched document
for further processing by other CIPs.

[1]: <https://github.com/nhs-ciao/ciao-docs-parser>

 

ciao-docs-enricher is built on top of [Apache Camel][2] and [Spring
Framework][3], and can be run as a stand-alone Java application, or via
[Docker][4].

[2]: <http://camel.apache.org/>

[3]: <http://projects.spring.io/spring-framework/>

[4]: <https://www.docker.com/>

Each application instance can host multiple [routes][5], where each route
follows the following basic structure:

[5]: <http://camel.apache.org/routes.html>

>   input JMS queue -\> DocumentEnricher or AsyncDocumentEnricher -\> output JMS
>   queue

The details of the JMS queues and document enrichers are specified at runtime
through a combination of [ciao-configuration][6] properties and spring XML
files.

[6]: <https://github.com/nhs-ciao/ciao-utils>

 

The following document enricher implementations are provided:

-   JsonResourceDocumentEnricher - An enricher which reads JSON content from the
    filesystem or classpath and merges it into the document properties. This can
    be used to include static content which is not available in the original
    source document.

-   PDSDocumentEnricher - An enricher which performs a PDS lookup based on
    properties previously extracted from the source document (e.g. NHS number),
    and merges the retrieved detail into the document properties.

For more advanced usages, a custom document enricher can be integrated by
implementing one of the required Java interfaces and providing a suitable spring
XML configuration on the classpath.

 

Configuration
-------------
