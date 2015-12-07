package de.monochromata.jactr.tls;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.jactr.core.chunk.basic.BasicSymbolicChunk;
import org.jactr.core.chunk.event.ChunkEvent;
import org.jactr.core.slot.ISlot;
import org.jactr.core.slot.NotifyingSlotContainer;
import org.jactr.core.slot.event.ISlotContainerListener;

public class SymbolicChunk extends BasicSymbolicChunk {

	NotifyingSlotContainer delegateContainer = new NotifyingSlotContainer();
	
	public SymbolicChunk() {
		super();
		delegateContainer.setDelegateContainer(this);
	}

	@Override
	public void addListener(ISlotContainerListener listener, Executor executor) {
		super.addListener(listener, executor);
		delegateContainer.addListener(listener, executor);
	}

	@Override
	public void removeListener(ISlotContainerListener listener) {
		super.removeListener(listener);
		delegateContainer.removeListener(listener);
	}

	@Override
	public void dispose() {
		super.dispose();
		delegateContainer.dispose();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void valueChanged(ISlot slot, Object oldValue, Object newValue) {
		// Use a delegate container, because super.super.valueChanged cannot
		// be invoked without invoking super.valueChanged which is what is
		// supposed to be avoided with this overriding method declaration.
		if(slot instanceof ICollectionSlot) {
			delegateContainer.valueChanged(slot, oldValue, newValue);
			if (_parentChunk.hasListeners())
			      _parentChunk.dispatch(new CollectionSlotValueChangedEvent(
			    		  _parentChunk, (ICollectionSlot)slot, (Collection<Object>)oldValue));
		} else {
			super.valueChanged(slot, oldValue, newValue);
		}
	}

	@Override
	protected ISlot createSlot(ISlot slot) {
		if (_useMutable)
			return new CollectionSlot(slot.getName(), slot.getValue(), this);

		return new CollectionSlot(slot.getName(), slot.getValue());
	}

}
