package de.monochromata.jactr.rm;

import java.util.Collection;
import java.util.Iterator;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.module.retrieval.six.DefaultRetrievalModule6;
import org.jactr.core.production.request.ChunkTypeRequest;

import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.twm.ITWM;

/**
 * A retrieval module that limits the chunks to be returned by a retrieval to
 * scopes that are identical to a set scope or enclose the set scope.
 *
 * @see Scope#isIdenticalOrEncloses(Scope)
 * @see #setScope(Scope)
 */
public class ScopedRetrievalModule6 extends DefaultRetrievalModule6 implements IScopedRetrievalModule {

	private Scope scope;

	/**
	 * Set the scope to apply when choosing the best match.
	 * 
	 * @param scope
	 *            The scope, may be {@code null} if no scope is to be checked.
	 * @see #selectRetrieval(Collection, IChunk, ChunkTypeRequest,
	 *      ChunkTypeRequest)
	 */
	@Override
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * Choose the best matching result from the collection. Note that if a scope
	 * has been set on the module, the choice is restricted to chunks within
	 * that scope or scopes enclosed by the set scope.
	 * 
	 * @param results
	 *            The results to choose from.
	 * @param errorChunk
	 *            The chunk to return if no results are left or an error
	 *            occurred.
	 * @see #setScope(Scope)
	 */
	@Override
	protected IChunk selectRetrieval(Collection<IChunk> results, IChunk errorChunk, ChunkTypeRequest originalRequest,
			ChunkTypeRequest cleanedRequest) {
		if (this.scope != null) {
			restrictResultsToChunksFromIdenticalOrEnclosingScopes(results);
		}
		return super.selectRetrieval(results, errorChunk, originalRequest, cleanedRequest);
	}

	protected void restrictResultsToChunksFromIdenticalOrEnclosingScopes(Collection<IChunk> results) {
		Iterator<IChunk> iterator = results.iterator();
		while (iterator.hasNext()) {
			removeChunkIfOutOfScope(iterator, iterator.next());
		}
	}

	protected void removeChunkIfOutOfScope(Iterator<IChunk> iterator, IChunk chunk) {
		Scope chunkScope = (Scope) chunk.getMetaData(ITWM.scope);
		if (chunkScope == null)
			throw new IllegalStateException("Chunk has no scope: " + chunk);
		if (!chunkScope.isIdenticalOrEncloses(this.scope))
			iterator.remove();
	}

}
