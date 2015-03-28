package de.monochromata.jactr.rm;

import org.jactr.core.module.retrieval.IRetrievalModule;

import de.monochromata.jactr.tls.Scope;

/**
 * A retrieval module that will only return chunks that
 * are within a given scope.
 * 
 * @see IRetrievalModule#retrieveChunk(org.jactr.core.production.request.ChunkTypeRequest)
 * @see #setScope(Scope)
 */
public interface IScopedRetrievalModule extends IRetrievalModule {
	
	/**
	 * Sets the scope that is applied to subsequent invocations
	 * of {@link IRetrievalModule#retrieveChunk(org.jactr.core.production.request.ChunkTypeRequest)}.
	 * 
	 * The passed scope will restrict the chunks retrieved to
	 * those that come from an identical or encloding scope.
	 * Passing null resets the scope so that no scope checking
	 * will be performed.
	 * 
	 * @param scope
	 * @see IRetrievalModule#retrieveChunk(org.jactr.core.production.request.ChunkTypeRequest)
	 * @see Scope#isIdenticalOrEncloses(Scope)
	 */
	public void setScope(Scope scope);
}
