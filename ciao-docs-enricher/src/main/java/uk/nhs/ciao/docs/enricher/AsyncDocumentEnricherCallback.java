package uk.nhs.ciao.docs.enricher;

import java.util.Map;

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
	void onDocumentWasEnriched(Map<String, Object> enrichedDocument);
	
	/**
	 * Callback invoked when document enrichment failed.
	 * 
	 * @param cause The cause of the failure
	 */
	void onDocumentEnrichmentFailed(Throwable cause);
}