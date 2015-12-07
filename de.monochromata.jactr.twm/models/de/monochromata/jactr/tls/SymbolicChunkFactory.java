package de.monochromata.jactr.tls;

import org.jactr.core.chunk.ISymbolicChunk;
import org.jactr.core.module.declarative.basic.chunk.DefaultSymbolicChunkFactory;
import org.jactr.core.slot.IMutableSlot;
import org.jactr.core.slot.ISlot;

public class SymbolicChunkFactory extends DefaultSymbolicChunkFactory {

	@Override
	public ISymbolicChunk newSymbolicChunk() {
		return new SymbolicChunk();
	}

	/**
	 * An implementation aware of {@link ICollectionSlot}s.
	 */
	public void copy(ISymbolicChunk source, ISymbolicChunk destination) {
		for (ISlot slot : source.getSlots()) {
			// this is the actual backing slot..
			IMutableSlot cs = (IMutableSlot) destination
					.getSlot(slot.getName());
			if(slot instanceof ICollectionSlot) {
				cs.setValue(((ICollectionSlot)slot).getValues());
			} else {
				cs.setValue(slot.getValue());
			}
		}
	}
}
