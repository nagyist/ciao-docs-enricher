package uk.nhs.ciao.docs.enricher;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Processor;
import org.apache.camel.util.AsyncProcessorHelper;

import uk.nhs.ciao.docs.parser.ParsedDocument;

import com.google.common.base.Preconditions;

public abstract class DocumentEnricherProcessor implements Processor {
	public static DocumentEnricherProcessor createProcessor(final Object enricher) {
		if (enricher instanceof DocumentEnricher) {
			return createSynchronousProcessor((DocumentEnricher)enricher);
		} else if (enricher instanceof AsyncDocumentEnricher) {
			return createAsynchronousProcessor((AsyncDocumentEnricher)enricher);
		}
		
		throw new IllegalArgumentException("invalid enricher type: " + enricher);
	}
	
	public static DocumentEnricherProcessor createSynchronousProcessor(final DocumentEnricher enricher) {
		return new SynchronousProcessor(enricher);
	}
	
	public static DocumentEnricherProcessor createAsynchronousProcessor(final AsyncDocumentEnricher enricher) {
		return new AsynchronousProcessor(enricher);
	}
	
	protected ParsedDocument getInputDocument(final Exchange exchange)
			throws InvalidPayloadException {
		return exchange.getIn().getMandatoryBody(ParsedDocument.class);
	}

	protected void setOutputDocument(final Exchange exchange, final ParsedDocument enrichedDocument) {
		exchange.getOut().copyFrom(exchange.getIn());
		exchange.getOut().setBody(enrichedDocument);
	}
	
	private static class SynchronousProcessor extends DocumentEnricherProcessor {
		private final DocumentEnricher enricher;
		
		public SynchronousProcessor(final DocumentEnricher enricher) {
			this.enricher = Preconditions.checkNotNull(enricher);
		}
		
		@Override
		public void process(final Exchange exchange) throws Exception {
			final ParsedDocument document = getInputDocument(exchange);
			final ParsedDocument enrichedDocument = enricher.enrichDocument(document);
			setOutputDocument(exchange, enrichedDocument);
		}
	}
	
	private static class AsynchronousProcessor extends DocumentEnricherProcessor implements AsyncProcessor {
		private final AsyncDocumentEnricher enricher;
		
		public AsynchronousProcessor(final AsyncDocumentEnricher enricher) {
			this.enricher = Preconditions.checkNotNull(enricher);
		}
		
		@Override
		public void process(final Exchange exchange) throws Exception {
			AsyncProcessorHelper.process(this, exchange);
		}

		@Override
		public boolean process(final Exchange exchange, final AsyncCallback callback) {
			final AsyncDocumentEnricherCallback enricherCallback = new AsyncDocumentEnricherCallback() {
				private final boolean doneSync = false;
				@Override
				public void onDocumentWasEnriched(final ParsedDocument enrichedDocument) {
					try {
						setOutputDocument(exchange, enrichedDocument);
						callback.done(doneSync);
					} catch (Exception e) {
						exchange.setException(e);
						callback.done(doneSync);
					}
				}
				
				@Override
				public void onDocumentEnrichmentFailed(final Throwable cause) {
					exchange.setException(cause);
					callback.done(doneSync);
				}
			};
			
			try {
				final ParsedDocument document = getInputDocument(exchange);				
				enricher.enrichDocument(document, enricherCallback);
				
			} catch (Exception e) {
				exchange.setException(e);
				final boolean doneSync = true;
				callback.done(doneSync);
				return false;
			}
			
			return true;
		}
	}
}
