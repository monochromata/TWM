package de.monochromata.jactr.twm;

import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.buffer.IActivationBuffer;
import org.jactr.core.buffer.delegate.AsynchronousRequestDelegate;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.module.IModule;
import org.jactr.core.production.request.IRequest;
import org.jactr.modules.pm.common.buffer.AbstractCapacityPMActivationBuffer6;

/**
 * A buffer that contains word and reference potential
 * chunks to spread activation during operation on TWM
 * nodes.
 * 
 * Note that this buffer cannot be accessed using requests
 * and is maintained by the request delegates of
 * {@link TWMBuffer}.
 * 
 * @see WordActivation
 * @see Referentialisation
 */
public class TWMGraphemicBuffer extends TWMCapacityBuffer {

	static transient Log LOGGER = LogFactory.getLog(TWMGraphemicBuffer.class);
	private final TWMModule module;
	
	public TWMGraphemicBuffer(String name, TWMModule module,
			int capacity) {
		super(name, module, capacity);
		this.module = module;
		setEjectionPolicy(EjectionPolicy.LeastRecentlyUsed);
	}

	@Override
	protected boolean isValidChunkType(IChunkType chunkType) {
		try {
			return module.getWordChunkType().equals(chunkType)
				|| module.getReferencePotentialChunkType().equals(chunkType);
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Cannot check if "+chunkType
					+" is a valid chunk type for TWMBuffer: "+e.getMessage(), e);
			return false;
		}
	}

	@Override
	protected void grabReferences() {
		super.grabReferences();
		addRequestDelegate(new RejectAllRequestDelegate());
	}
}

class RejectAllRequestDelegate extends AsynchronousRequestDelegate {

	@Override
	public boolean willAccept(IRequest request) {
		TWMGraphemicBuffer.LOGGER.warn("Rejecting request "+request);
		return false;
	}

	@Override
	protected boolean isValid(IRequest request, IActivationBuffer buffer)
			throws IllegalArgumentException {
		return false;
	}

	@Override
	protected Object startRequest(IRequest request, IActivationBuffer buffer,
			double requestTime) {
		return null;
	}

	@Override
	protected void finishRequest(IRequest request, IActivationBuffer buffer,
			Object startValue) {
	}
	
}