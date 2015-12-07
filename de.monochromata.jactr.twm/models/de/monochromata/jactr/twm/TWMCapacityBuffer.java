package de.monochromata.jactr.twm;

import org.jactr.core.chunk.IChunk;
import org.jactr.modules.pm.common.buffer.AbstractCapacityPMActivationBuffer6;

/**
 * This buffer ensures that the insertion
 * time of a chunk is updated when an attempt is made to add
 * the chunk to the buffer again (even though it is already
 * contained in the buffer). 
 */
public abstract class TWMCapacityBuffer extends AbstractCapacityPMActivationBuffer6 {

	private int capacity;
	
	public TWMCapacityBuffer(String name, TWMModule module,
			int capacity) {
		super(name, module);
		this.capacity = capacity;
	}

	@Override
	protected boolean isCapacityReached() {
		getLock().readLock().lock();
		boolean capacityReached = getTimesAndChunks().size() > capacity;
		getLock().readLock().unlock();
		return capacityReached;
	}

	@Override
	protected boolean shouldCopyOnInsertion(IChunk chunk) {
		return false;
	}

	/**
	 * The buffer (actually {@link WordActivation} and
	 * {@link Referentialisation} handles encoding, removal
	 * of source chunks should not trigger their encoding. 
	 */
	@Override
	public boolean handlesEncoding() {
		return true;
	}

	/**
	 * The implementation of this method ensures that the insertion
	 * time of the given source chunk is updated if it is already
	 * contained in the buffer.
	 */
	@Override
	public IChunk addSourceChunk(IChunk sourceChunk) {
		
		// Update the insertion time of the given chunk, in case
		// it is already contained in the buffer.
		if(sourceChunk != null && containsExact(sourceChunk) != null) {
			matchedInternal(sourceChunk);
		}
		
		return super.addSourceChunk(sourceChunk);
	}

	
	
}
