package uk.nhs.ciao.docs.enricher;

import uk.nhs.ciao.docs.parser.ParsedDocument;

/**
 * An asynchronous callback to notify when document enrichment has completed.
 * 
 * @see AsyncDocumentEnricher
 */
public interface AsyncDocumentEnricherCallback {
	/**
	 * Callback invoked when the document has been successfully enriched
	 * 
	 * @param enrichedDocument The enriched document
	 */
	void onDocumentWasEnriched(ParsedDocument enrichedDocument);
	
	/**
	 * Callback invoked when document enrichment failed.
	 * 
	 * @param cause The cause of the failure
	 */
	void onDocumentEnrichmentFailed(Throwable cause);
}