package uk.nhs.ciao.docs.enricher;

import uk.nhs.ciao.docs.parser.ParsedDocument;

/**
 * Enriches a document with additional properties in an asynchronous manner
 * 
 * @see DocumentEnricher
 */
public interface AsyncDocumentEnricher {
	/**
	 * Enriches the document asynchronously.
	 * <p>
	 * Enricher instances should notify the caller of asynchronous completion via the provided callback.
	 * 
	 * @param document The document to enrich
	 * @param asyncCallback The callback to notify once enrichment has been completed or has failed
	 * @throws Exception If the document could not be processes during the initial synchronous stage of enrichment
	 */
	void enrichDocument(final ParsedDocument document, final AsyncDocumentEnricherCallback asyncCallback) throws Exception;
}
