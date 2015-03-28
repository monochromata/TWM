package de.monochromata.jactr.dl;

import java.util.concurrent.Executor;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunk.event.IChunkListener;
import org.jactr.core.chunk.four.ISubsymbolicChunk4;
import org.jactr.core.chunk.four.Link4;
import org.jactr.core.chunk.link.IAssociativeLink;
import org.jactr.core.module.declarative.associative.IAssociativeLinkageSystem;
import org.jactr.core.module.declarative.event.DeclarativeModuleEvent;
import org.jactr.core.module.declarative.event.DeclarativeModuleListenerAdaptor;
import org.jactr.core.module.declarative.event.IDeclarativeModuleListener;
import org.jactr.core.module.declarative.four.learning.IDeclarativeLearningModule4;
import org.jactr.core.module.declarative.six.associative.DefaultAssociativeLinkageSystem6;
import org.jactr.core.module.declarative.six.learning.IDeclarativeLearningModule6;
import org.jactr.core.slot.ISlot;

import de.monochromata.jactr.tls.CollectionSlot;
import de.monochromata.jactr.tls.ICollectionSlot;

/**
 * An associative linkage system that can maintain links using
 * {@link CollectionSlot}s.
 */
public class CollectionAssociativeLinkageSystem extends
		DefaultAssociativeLinkageSystem6 {

	private static final transient Log LOGGER = LogFactory
			.getLog(CollectionAssociativeLinkageSystem.class);

	public CollectionAssociativeLinkageSystem(
			IDeclarativeLearningModule6 learningModule, Executor executor) {
		super(learningModule, executor);
	}

	/**
	 * Create an {@link IDeclarativeModuleListener} that is able to
	 * react to the creation of chunks that have {@link ICollectionSlot}s.
	 */
	protected IDeclarativeModuleListener createDeclarativeModuleListener(
			IDeclarativeLearningModule4 learningModule, Executor executor) {
		return new DeclarativeModuleListenerAdaptor() {

			@Override
			public void chunkCreated(DeclarativeModuleEvent dme) {
				IChunk iChunk = dme.getChunk();

				/*
				 * create the initial self-link
				 */
				IAssociativeLinkageSystem linkageSystem = dme.getSource()
						.getAssociativeLinkageSystem();

				if (linkageSystem != null) {
					ISubsymbolicChunk4 ssc4 = iChunk
							.getAdapter(ISubsymbolicChunk4.class);
					ssc4.addLink(linkageSystem.createLink(iChunk, iChunk));
				}

				/*
				 * chunks with default slot values will by-pass the normal chain
				 * of event notification (the slot values are assigned before
				 * the chunk listener can be attached). So, we do the
				 * containment linking directly
				 */

				FastList<ISlot> slots = FastList.newInstance();
				try {
					iChunk.getSymbolicChunk().getSlots(slots);

					if (slots.size() == 0)
						return;

					if (LOGGER.isDebugEnabled())
						LOGGER.debug(String.format("Post-linking %s and %s",
								iChunk, slots));

					for (ISlot slot : slots) {
						if(slot instanceof ICollectionSlot) {
							ICollectionSlot cSlot = (ICollectionSlot)slot;
							for(Object value: cSlot.getValues()) {
								if(value instanceof IChunk) {
									// add the new link
									linkSlotValue(iChunk, (IChunk)value, false);
								}
							}
						} else if(slot.getValue() instanceof IChunk) {
							linkSlotValue(iChunk, (IChunk) slot.getValue(),
									false);
						}
					}
				} finally {
					FastList.recycle(slots);
				}
			}
		};
	}

	/**
	 * Create an {@link IChunkListener} that is able to handle chunks
	 * that have {@link ICollectionSlot}s.
	 */
	@Override
	protected IChunkListener createChunkListener(
			IDeclarativeLearningModule4 learningModule, Executor executor) {
		return new CollectionChunkListener(this);
	}

	@Override
	protected void linkSlotValue(IChunk iChunk, IChunk value, boolean valueIsOld) {
		// Override to make visible in this package
		super.linkSlotValue(iChunk, value, valueIsOld);
	}


}
