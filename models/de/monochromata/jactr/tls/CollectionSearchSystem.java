package de.monochromata.jactr.tls;

import java.util.Collection;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.module.declarative.IDeclarativeModule;
import org.jactr.core.module.declarative.search.local.DefaultSearchSystem;
import org.jactr.core.module.declarative.search.map.ITypeValueMap;
import org.jactr.core.slot.ISlot;

/**
 * A search system that is able to index {@link ICollectionSlot}s and perform
 * searches on them.
 */
public class CollectionSearchSystem extends DefaultSearchSystem {

	public CollectionSearchSystem(IDeclarativeModule module) {
		super(module);
	}
	
	// Note that the methods
	// guessSize, find, equals, guessEqualsSize,
	// lessThan, guessLessThanSize, greaterThan, guessGreaterThanSize
	// not, guessNotSize do not operate on ICollectionSlots but on
	// LogicalSlot instances. Invocations of getValue() do not need and
	// cannot be replaced by invocations of getValues(). The
	// CollectionTypeValueMap needs to expect both single values and
	// collections of values on its operations and maintain a core
	// index for single values (either provided directly or extracted
	// from collections).

	@Override
	public void index(IChunk chunk) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Indexing " + chunk);

		if (!chunk.isEncoded())
			throw new RuntimeException(chunk
					+ " has not been encoded, will not index");

		for (ISlot slot : chunk.getSymbolicChunk().getSlots()) {
			Object value = slot instanceof ICollectionSlot?
					((ICollectionSlot) slot).getValues():slot.getValue();
			addIndexing(chunk, slot.getName(), value);
		}
	}

	@Override
	public void unindex(IChunk chunk) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Unindexing " + chunk);

		for (ISlot slot : chunk.getSymbolicChunk().getSlots()) {
			Object value = slot instanceof ICollectionSlot?
					((ICollectionSlot)slot).getValue():slot.getValue();
			removeIndexing(chunk, slot.getName(), value);
		}
	}

	@Override
	protected ITypeValueMap<?, IChunk> instantiateTypeValueMap(Object value) {
		if (value != null && value instanceof Collection) {
			return new CollectionTypeValueMap<IChunk>();
		} else {
			return super.instantiateTypeValueMap(value);
		}
	}

}
