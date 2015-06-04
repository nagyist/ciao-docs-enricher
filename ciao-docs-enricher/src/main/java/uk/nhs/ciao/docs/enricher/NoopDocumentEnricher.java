package uk.nhs.ciao.docs.enricher;

import uk.nhs.ciao.docs.parser.ParsedDocument;

/**
 * A document enricher that acts as a simple pass-through
 */
public class NoopDocumentEnricher implements DocumentEnricher {
	/**
	 * The incoming document is returned with no changes
	 */
	@Override
	public ParsedDocument enrichDocument(final ParsedDocument document) {
		return document;
	}
}
