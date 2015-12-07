package de.monochromata.jactr.tls;

import java.util.Collection;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.event.ChunkEvent;

/**
 * A slot change event that invokes {@link ICollectionSlot#getValues()}
 * instead of {@link ISlot#getValue()}.
 */
public class CollectionSlotValueChangedEvent extends ChunkEvent {

	private final String slotName;
	private final Collection<Object> oldValue, newValue;
	
	public CollectionSlotValueChangedEvent(IChunk source,
			ICollectionSlot slot, Collection<Object> oldValue) {
		super(source, Type.SLOT_VALUE_CHANGED);
		slotName = slot.getName();
		this.oldValue = oldValue;
		this.newValue = slot.getValues();
	}

	@Override
	public String getSlotName() {
		return slotName;
	}

	@Override
	public Object getNewSlotValue() {
		return newValue;
	}

	@Override
	public Object getOldSlotValue() {
		return oldValue;
	}

	
	
}
