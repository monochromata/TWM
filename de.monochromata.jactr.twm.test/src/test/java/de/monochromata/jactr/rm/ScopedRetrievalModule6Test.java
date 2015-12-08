package de.monochromata.jactr.rm;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.production.request.ChunkTypeRequest;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import de.monochromata.jactr.tls.Scope;
import de.monochromata.jactr.twm.ITWM;

public class ScopedRetrievalModule6Test {

	@Rule
	public final JUnitRuleMockery context = new JUnitRuleMockery();

	protected final ScopedRetrievalModule6 srm = new ScopedRetrievalModule6();

	protected final Scope scope = Scope.create("a.b");
	protected final Scope identicalScope = Scope.create(new String("a.b"));
	protected final Scope enclosingScope = Scope.create("a");
	protected final Scope outOfScope = Scope.create("whereever");

	protected final IChunk identicalScopeChunk = context.mock(IChunk.class, "identicalScopeChunk");
	protected final IChunk enclosingScopeChunk = context.mock(IChunk.class, "enclosingScopeChunk");
	protected final IChunk outOfScopeChunk = context.mock(IChunk.class, "outOfScopeChunk");
	protected final IChunk chunkWithoutScope = context.mock(IChunk.class, "chunkWithoutScope");

	@SuppressWarnings("unchecked")
	protected final Collection<IChunk> results = context.mock(Collection.class, "results");
	@SuppressWarnings("unchecked")
	protected final Iterator<IChunk> iterator = context.mock(Iterator.class);
	protected final IChunk errorChunk = context.mock(IChunk.class, "errorChunk");
	protected final IChunkType chunkType = context.mock(IChunkType.class);
	protected final ChunkTypeRequest originalRequest = new ChunkTypeRequest(chunkType);
	protected final ChunkTypeRequest cleanedRequest = new ChunkTypeRequest(chunkType);

	@Test
	public void nullScopeMustNotShrinkResults() {

		context.checking(new Expectations() {
			{
				oneOf(results).size();
				will(returnValue(3));
				oneOf(results).iterator();
				will(returnValue(iterator));
				oneOf(iterator).next();
				will(returnValue(identicalScopeChunk));

				exactly(0).of(iterator).remove();
			}
		});

		IChunk retrievedChunk = srm.selectRetrieval(results, errorChunk, originalRequest, cleanedRequest);

		assertThat(retrievedChunk, sameInstance(identicalScopeChunk));
	}

	@Test(expected = IllegalStateException.class)
	public void chunkWithoutScopeRaisesIllegalStateException() {
		context.checking(new Expectations() {
			{
				oneOf(results).iterator();
				will(returnValue(iterator));
				exactly(1).of(iterator).hasNext();
				will(returnValue(true));
				oneOf(iterator).next();
				will(returnValue(identicalScopeChunk));
				oneOf(identicalScopeChunk).getMetaData(ITWM.scope);

				will(returnValue(null));
			}
		});

		srm.setScope(scope);

		// Throws IllegalStateException
		srm.selectRetrieval(results, errorChunk, originalRequest, cleanedRequest);
	}

	@Test
	public void chunkWithIdenticalScopeMustPass() {
		testPassingChunk(identicalScopeChunk, identicalScope);
	}

	@Test
	public void chunkWithEnclosingScopeMustPass() {
		testPassingChunk(enclosingScopeChunk, enclosingScope);
	}

	protected void testPassingChunk(IChunk passingChunk, Scope passingChunkScope) {
		context.checking(new Expectations() {
			{
				oneOf(results).iterator();
				will(returnValue(iterator));
				oneOf(iterator).hasNext();
				will(returnValue(true));
				oneOf(iterator).next();
				will(returnValue(passingChunk));
				oneOf(passingChunk).getMetaData(ITWM.scope);
				will(returnValue(passingChunkScope));
				oneOf(iterator).hasNext();
				will(returnValue(false));

				oneOf(results).size();
				will(returnValue(1));
				oneOf(results).iterator();
				will(returnIterator(Arrays.asList(passingChunk)));

				exactly(0).of(iterator).remove();
			}
		});

		srm.setScope(scope);
		IChunk retrievedChunk = srm.selectRetrieval(results, errorChunk, originalRequest, cleanedRequest);

		assertThat(retrievedChunk, sameInstance(passingChunk));
	}

	@Test
	public void outOfScopeChunkMustBeRemoved() {
		context.checking(new Expectations() {
			{
				oneOf(results).iterator();
				will(returnValue(iterator));
				exactly(2).of(iterator).hasNext();
				will(returnValue(true));

				oneOf(iterator).next();
				will(returnValue(outOfScopeChunk));
				oneOf(outOfScopeChunk).getMetaData(ITWM.scope);
				will(returnValue(outOfScope));

				oneOf(iterator).next();
				will(returnValue(identicalScopeChunk));
				oneOf(identicalScopeChunk).getMetaData(ITWM.scope);
				will(returnValue(identicalScope));

				oneOf(iterator).hasNext();
				will(returnValue(false));

				oneOf(results).size();
				will(returnValue(1));
				oneOf(results).iterator();
				will(returnIterator(Arrays.asList(identicalScopeChunk)));

				exactly(1).of(iterator).remove();
			}
		});

		srm.setScope(scope);
		IChunk retrievedChunk = srm.selectRetrieval(results, errorChunk, originalRequest, cleanedRequest);

		assertThat(retrievedChunk, sameInstance(identicalScopeChunk));
	}

}
