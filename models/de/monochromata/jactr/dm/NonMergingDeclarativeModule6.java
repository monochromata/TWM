package de.monochromata.jactr.dm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.ISymbolicChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.module.declarative.search.filter.IChunkFilter;
import org.jactr.core.module.declarative.six.DefaultDeclarativeModule6;
import org.jactr.core.production.request.ChunkTypeRequest;
import org.jactr.core.slot.ISlot;

import de.monochromata.jactr.tls.CollectionSearchSystem;

/**
 * A declarative module that does not merge added chunks to existing chunks
 * and uses a {@link CollectionSearchSystem}.
 *
 */
public class NonMergingDeclarativeModule6 extends DefaultDeclarativeModule6
		implements INonMergingDeclarativeModule {

	private static final transient Log LOGGER = LogFactory
			.getLog(NonMergingDeclarativeModule6.class);

	
	
	public NonMergingDeclarativeModule6() {
		super();
		_searchSystem = new CollectionSearchSystem(this);
	}

	/**
	 * add chunk to DM without performing a search for potential matches.
	 * Delegates to {@link #addChunkInternal(IChunk, Collection)} on
	 * {@link #getExecutor()}
	 * 
	 * @param chunk
	 * @return
	 * @see org.jactr.core.module.declarative.IDeclarativeModule#addChunk(org.jactr.core.chunk.IChunk)
	 */
	public CompletableFuture<IChunk> addChunk(final IChunk chunk) {
		if (chunk.isEncoded()) {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(chunk + " has already been encoded, silly");
			return immediateReturn(chunk);
		}

		ISymbolicChunk sc = chunk.getSymbolicChunk();
		String name = sc.getName();
		if ((getBusyChunk() == null && name.equals("busy"))
				|| (getEmptyChunk() == null && name.equals("empty"))
				|| (getErrorChunk() == null && name.equals("error"))
				|| (getFreeChunk() == null && name.equals("free"))
				|| (getFullChunk() == null && name.equals("full"))
				|| (getNewChunk() == null && name.equals("new"))
				|| (getRequestedChunk() == null && name.equals("requested"))
				|| (getUnrequestedChunk() == null && name.equals("unrequested"))) {
			// Delegate to the super class, if the chunk is a special chunk
			// and these chunks have not yet been encoded.
			return NonMergingDeclarativeModule6.super.addChunk(chunk);
		} else {
			return delayedFuture(new Callable<IChunk>() {

				public IChunk call() throws Exception {
					try {
						return addChunkInternal(chunk, Collections.emptySet());
					} catch (Exception e) {
						LOGGER.error("Error while encoding chunk " + chunk
								+ " ", e);
						throw e;
					}
				}

			}, getExecutor());
		}
	}

	/**
	 * Ignores the passed possible matches and invokes the overwritten
	 * implementation using an empty set of possible matches for a merge.
	 */
	@Override
	protected IChunk addChunkInternal(IChunk chunk,
			Collection<IChunk> possibleMatches) {
		return super.addChunkInternal(chunk, Collections.emptySet());
	}

}
