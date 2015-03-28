package de.monochromata.jactr.dl;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.event.ChunkEvent;
import org.jactr.core.module.declarative.four.associative.ChunkListener;
import de.monochromata.jactr.tls.ICollectionSlot;

/**
 * The chunk listener used by {@link CollectionAssociativeLinkageSystem} to
 * react to changes to chunks that use {@link ICollectionSlot}s.
 */
public class CollectionChunkListener extends ChunkListener {

	private static final transient Log LOGGER = LogFactory
			.getLog(CollectionChunkListener.class);

	private final CollectionAssociativeLinkageSystem linkageSystem;

	public CollectionChunkListener(
			CollectionAssociativeLinkageSystem linkageSystem) {
		super(linkageSystem);
		this.linkageSystem = linkageSystem;
	}

	/**
	 * @throws UnsupportedOperationException
	 *             Always thrown by this implementation, because three-level
	 *             semantics currently should be used without merging.
	 */
	@Override
	public void mergingInto(ChunkEvent event) {
		throw new UnsupportedOperationException("Merging is not currently"
				+ " supported in three-level semantics");
	}

	@Override
	public void slotChanged(ChunkEvent ce) {
		IChunk iChunk = ce.getSource();
		Object oldValue = ce.getOldSlotValue();
		Object newValue = ce.getNewSlotValue();

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(iChunk + "." + ce.getSlotName() + "=" + newValue
					+ " (was " + oldValue + ")");

		linkSlotValue(iChunk, oldValue, true);
		linkSlotValue(iChunk, newValue, false);
	}

	@SuppressWarnings("unchecked")
	protected void linkSlotValue(IChunk iChunk, Object value, boolean valueIsOld) {
		if (value instanceof Collection) {
			for(Object element: (Collection<Object>)value) {
				if(element instanceof IChunk) {
					linkSlotValue(iChunk, (IChunk)element, valueIsOld);
				}
			}
		} else if (value instanceof IChunk) {
			linkSlotValue(iChunk, (IChunk) value, valueIsOld);
		}
	}
	
	private void linkSlotValue(IChunk iChunk, IChunk value, boolean valueIsOld) {
		linkageSystem.linkSlotValue(iChunk, value, valueIsOld);
	}

}
