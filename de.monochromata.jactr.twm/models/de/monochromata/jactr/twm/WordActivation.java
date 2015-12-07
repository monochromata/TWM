package de.monochromata.jactr.twm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.buffer.IActivationBuffer;
import org.jactr.core.buffer.delegate.AsynchronousRequestDelegate;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.ISubsymbolicChunk;
import org.jactr.core.chunk.ISymbolicChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.module.declarative.IDeclarativeModule;
import org.jactr.core.module.declarative.search.filter.ActivationFilter;
import org.jactr.core.module.retrieval.IRetrievalModule;
import org.jactr.core.production.request.ChunkRequest;
import org.jactr.core.production.request.ChunkTypeRequest;
import org.jactr.core.production.request.IRequest;
import org.jactr.core.queue.ITimedEvent;
import org.jactr.core.queue.timedevents.AbstractTimedEvent;
import org.jactr.core.queue.timedevents.IBufferBasedTimedEvent;
import org.jactr.core.slot.BasicSlot;
import org.jactr.core.slot.DefaultConditionalSlot;
import org.jactr.core.slot.DefaultMutableSlot;
import org.jactr.core.slot.ISlot;

import de.monochromata.jactr.dm.INonMergingDeclarativeModule;
import de.monochromata.jactr.rm.IScopedRetrievalModule;
import de.monochromata.jactr.tls.ICollectionSlot;
import de.monochromata.jactr.tls.Scope;
import static de.monochromata.jactr.twm.TWMModule.doLocked;

/**
 * Adds requested {@link Word} instances to the {@link TWMGraphemicBuffer} to
 * cause them to spread activation.
 */
public class WordActivation extends AsynchronousRequestDelegate {

	private static final transient Log LOGGER = LogFactory.getLog(WordActivation.class);
	protected final TWMModule module;
	protected final TWMBuffer buffer;
	protected final TWMGraphemicBuffer graphemicBuffer;
	protected final double schemaCreationDuration;
	protected final IChunkType wordChunkType, referencePotentialChunkType, lexicalizedConceptualSchemaChunkType;
	
	public WordActivation(TWMModule module, TWMBuffer buffer,
			TWMGraphemicBuffer graphemicBuffer)
			throws InterruptedException, ExecutionException {
		super();
		this.module = module;
		this.buffer = buffer;
		this.graphemicBuffer = graphemicBuffer;
		this.schemaCreationDuration = module.getSchemaCreationDuration();
		this.wordChunkType = module.getWordChunkType();
		this.referencePotentialChunkType = module.getReferencePotentialChunkType();
		this.lexicalizedConceptualSchemaChunkType = module.getLexicalizedConceptualSchemaChunkType();
		setAsynchronous(true);
		setUseBlockingTimedEvents(false);
		setDelayStart(false);
	}

	/**
	 * Only {@link ChunkRequest}s are accepted, because the meta-data
	 * provided by REMMA is required to operate the {@link TWMModule}.
	 */
	@Override
	public boolean willAccept(IRequest request) {
		return request != null
				&& request instanceof ChunkRequest
				&& ((ChunkRequest)request).getChunkType().isA(wordChunkType)
				&& !((ChunkRequest)request).getChunkType().isA(referencePotentialChunkType);
	}

	@Override
	public void clear() {
		super.clear();
	    ITimedEvent previous = getCurrentTimedEvent();
	    if (previous != null && !previous.hasAborted() && !previous.hasFired())
	      previous.abort();
	}

	@Override
	protected boolean isValid(IRequest request, IActivationBuffer buffer)
			throws IllegalArgumentException {
		if(isBusy(buffer)) {
			LOGGER.debug("TWM is busy, cannot process request on twm buffer");
			return false;
		} else if(((ChunkRequest)request).getChunk().getMetaData(ITWM.scope) == null) {
			LOGGER.debug("Chunk of chunk request has no scope meta data: "+request);
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	protected double computeCompletionTime(double startTime, IRequest request,
			IActivationBuffer buffer) {
		// The super method shall not be overwritten because it specifies a 50ms
		// delay that in conjunction with setDelayStart(false)in the constructor
		// ensures that the request is not executed before the production has fired
		// (i.e. after 50ms).
		return super.computeCompletionTime(startTime, request, buffer);
	}

	@Override
	protected Object startRequest(IRequest request, IActivationBuffer buffer,
			double requestTime) {
		
		// Ensure that the request chunk is the last element in the buffer
		IChunk wordChunk = ((ChunkRequest)request).getChunk();
		if(!buffer.getSourceChunks().contains(wordChunk)) {
			// Ensure that the request chunk spreads activation because it
			// might not be contained in another buffer.
			buffer.addSourceChunk(wordChunk);
		}
		
		module.fireRequestWordActivation(requestTime, wordChunk);
		
		setBusy(buffer);
		return new WordData(wordChunk);
	}

	@Override
	protected void abortRequest(IRequest request, IActivationBuffer buffer,
			Object startValue) {
		WordData data = (WordData)startValue;
		buffer.removeSourceChunk(data.requestChunk);
		setFree(buffer);
		LOGGER.debug("Aborting request="+request);
		super.abortRequest(request, buffer, startValue);
	}
	
	protected double schemaCreationDuration() {
		LOGGER.debug("retrieval: +"+schemaCreationDuration+" (create)");
		return schemaCreationDuration;
	}
	
	protected double schemaAccessDuration(IScopedRetrievalModule rm,
			IChunk schema) {
		double retrievalDuration = rm.getRetrievalTimeEquation().computeRetrievalTime(schema);
		ISubsymbolicChunk ssc = schema.getSubsymbolicChunk();
		LOGGER.debug("retrieval: +"+retrievalDuration
				+" (act="+ssc.getActivation()+", "+ssc.getReferences().getNumberOfReferences()+"x)"
				+" for "+schema.getSymbolicChunk());
		return retrievalDuration;
	}

	@Override
	protected void postStart(IRequest request, IActivationBuffer buffer,
			double startTime, double finishTime, Object startReturn) {
		WordData wordData = (WordData)startReturn;
		wordData.startTime = finishTime;
	}

	@Override
	protected void finishRequest(IRequest request, IActivationBuffer buffer,
			Object startValue) {
		WordData wordData = (WordData)startValue;
		
		// Find matching type schema
		ISymbolicChunk sc = wordData.requestChunk.getSymbolicChunk();
		Scope scope = (Scope)wordData.requestChunk.getMetaData(ITWM.scope);
		wordData.typeSchema = getBestMatchingType(sc, scope);
		
		// Compute retrieval duration for cached schema
		IScopedRetrievalModule retrievalModule = (IScopedRetrievalModule)
				module.getModel().getModule(IScopedRetrievalModule.class);
		double retrievalDuration = schemaAccessDuration(retrievalModule, wordData.typeSchema);
		wordData.endTime = wordData.startTime + retrievalDuration;
		
		// Queue request completion
		CompleteRequestTimedEvent event =
				new CompleteRequestTimedEvent(wordData.startTime, wordData.endTime,
						request, buffer, wordData);
		setCurrentTimedEvent(event);
		buffer.getModel().getTimedEventQueue().enqueue(event);
	}
	
	protected void completeRequest(IRequest request, IActivationBuffer buffer,
			WordData wordData) {
		
		// Move the request chunk from the twm to the twmGraphemic buffer
		// so it continues to spread activation from there after the production
		// finished.
		
		buffer.removeSourceChunk(wordData.requestChunk);
		graphemicBuffer.addSourceChunk(wordData.requestChunk);
		doLocked(wordData.typeSchema, c -> {
			c.getSubsymbolicChunk().accessed(wordData.endTime);
		});
		setFree(buffer);
	}
	
	protected IChunk getBestMatchingSchema(ISymbolicChunk sc, Scope scope,
			String typeVsToken) {
		
		IScopedRetrievalModule rm = (IScopedRetrievalModule)
				module.getModel().getModule(IScopedRetrievalModule.class);

		// Create chunk type request
		Collection<Object> graphemicValues =
				((ICollectionSlot)sc.getSlot("graphemic")).getValues();
		if(graphemicValues.size() != 1) {
			throw new IllegalStateException(
					"Illegal number of graphemic values in "+graphemicValues);
		}
		String graphemic = (String)graphemicValues.iterator().next();
		ISlot graphemicSlot = new BasicSlot("graphemic", graphemic);
		ISlot typeVsTokenSlot = new BasicSlot("typeVsToken", typeVsToken);
		ChunkTypeRequest schemaRequest = new ChunkTypeRequest(lexicalizedConceptualSchemaChunkType,
				Arrays.asList(new ISlot[] { graphemicSlot, typeVsTokenSlot }));

		// Put scope into effect
		rm.setScope(scope);
		
		// Only chunks exceeding retrieval threshold are returned.
		// Note that the graphemic value in the word chunk in twm buffer that
		// triggered this request does already spread activation to potential
		// matches.
		IChunk schema;
		try {
			schema = rm.retrieveChunk(schemaRequest).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Failed to retrieve schema "+schemaRequest+": "+e.getMessage(), e);
			schema = null;
		}
		
		// Remove scope
		rm.setScope(null);
		
		// Note that an access should be recorded for the schema when the 
		// request that caused the invocation of this method is finished.
		// When an access to a token schema is recorded, an access to
		// the/a corresponding type schema might be recorded too, to generate
		// a base-level effect that lasts longer than spreading activation.
		
		LOGGER.debug("schema="+schema.getSymbolicChunk());
		
		return schema;
	}
	
	protected IChunk getBestMatchingToken(ISymbolicChunk sc, Scope scope) {
		return getBestMatchingSchema(sc, scope, "token");
	}
	
	protected IChunk getBestMatchingType(ISymbolicChunk sc, Scope scope) {
		return getBestMatchingSchema(sc, scope, "type");
	}

	protected class WordData {
		
		protected final IChunk requestChunk;
		protected IChunk typeSchema;
		protected double startTime, endTime;
		
		protected WordData(IChunk requestChunk) {
			this.requestChunk = requestChunk;
		}
	}
	
	protected class CompleteRequestTimedEvent extends AbstractTimedEvent
		implements IBufferBasedTimedEvent {

		private final IRequest request;
		private final IActivationBuffer buffer;
		private final WordData wordData;
		
		public CompleteRequestTimedEvent(double start, double end, IRequest request,
				IActivationBuffer buffer, WordData wordData) {
			super(start, end);
			LOGGER.debug((end-start)+"s to complete "+request
					+(request instanceof ChunkRequest?(" with "+((ChunkRequest)request).getChunk()):""));
			this.request = request;
			this.buffer = buffer;
			this.wordData = wordData;
		}

		@Override
		public IActivationBuffer getBuffer() {
			return buffer;
		}

		@Override
		public IChunk getBoundChunk() {
			return null;
		}

		@Override
		public synchronized void fire(double currentTime) {
			super.fire(currentTime);
			completeRequest(request, buffer, wordData);
		}

		@Override
		public synchronized void abort() {
			super.abort();
			abortRequest(request, buffer, wordData);
		}
		
	}
}
