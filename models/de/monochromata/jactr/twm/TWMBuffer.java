package de.monochromata.jactr.twm;

import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.module.IModule;
import org.jactr.modules.pm.common.buffer.AbstractCapacityPMActivationBuffer6;

/**
 * A buffer that contains a number of TWM nodes and causes
 * activation to spread from these nodes.
 */
public class TWMBuffer extends TWMCapacityBuffer {

	private static transient Log LOGGER = LogFactory.getLog(TWMBuffer.class);
	private final TWMModule module;
	private final TWMGraphemicBuffer graphemicBuffer;
	
	public TWMBuffer(String name, TWMModule module,
			int capacity, TWMGraphemicBuffer graphemicBuffer) {
		super(name, module, capacity);
		this.module = module;
		this.graphemicBuffer = graphemicBuffer;
		setEjectionPolicy(EjectionPolicy.LeastRecentlyUsed);
	}

	@Override
	protected boolean isValidChunkType(IChunkType chunkType) {
		try {
			return module.getWordChunkType().equals(chunkType)
					|| module.getReferencePotentialChunkType().equals(chunkType)
					|| module.getLexicalizedConceptualSchemaChunkType().equals(chunkType)
					|| module.getConceptualSchemaChunkType().equals(chunkType);
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Cannot check if "+chunkType
					+" is a valid chunk type for TWMBuffer: "+e.getMessage(), e);
			return false;
		}
	}
	
	@Override
	protected void grabReferences() {
		super.grabReferences();
		try {
			addRequestDelegate(new WordActivation(module, this, graphemicBuffer));
			addRequestDelegate(new Referentialisation(module, this, graphemicBuffer));
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("Failed to create twem buffer request delegates: "+e.getMessage(), e);
		}
	}

}
