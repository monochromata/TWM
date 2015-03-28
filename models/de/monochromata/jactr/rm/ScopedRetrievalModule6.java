package de.monochromata.jactr.rm;

import java.util.Collection;
import java.util.Iterator;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.module.retrieval.six.DefaultRetrievalModule6;
import org.jactr.core.production.request.ChunkTypeRequest;

import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.twm.ITWM;

/**
 * A retrieval module that limits the chunks to be returned by a
 * retrieval to scope that are identical to a set scope or enclose
 * the set scope.
 *
 * @see Scope#isIdenticalOrEncloses(Scope)
 * @see #setScope(Scope)
 */
public class ScopedRetrievalModule6 extends DefaultRetrievalModule6 
	implements IScopedRetrievalModule {

	private Scope scope;
	
	@Override
	public void setScope(Scope scope) {
		this.scope = scope;
	}
	
	@Override
	protected IChunk selectRetrieval(Collection<IChunk> results,
			IChunk errorChunk, ChunkTypeRequest originalRequest,
			ChunkTypeRequest cleanedRequest) {
		
		// If a scope is in effect, restrict results to chunks
		// from identical or enclosing scopes.
		if(this.scope != null) {
			Iterator<IChunk> iterator = results.iterator();
			while(iterator.hasNext()) {
				IChunk chunk = iterator.next();
				Scope scope = (Scope)chunk.getMetaData(ITWM.scope);
				if(scope == null)
					throw new IllegalStateException("Chunk has no scope: "+chunk);
				if(!scope.isIdenticalOrEncloses(this.scope))
					iterator.remove();
			}
		}
		
		// Select the chunk to return
		return super.selectRetrieval(results, errorChunk, originalRequest,
				cleanedRequest);
	}

}
