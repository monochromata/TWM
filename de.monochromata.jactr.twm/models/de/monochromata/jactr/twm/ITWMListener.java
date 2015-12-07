package de.monochromata.jactr.twm;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.slot.ISlot;

public interface ITWMListener {
	
	public void requestWordActivation(double requestTime, IChunk wordChunk);
	
	public void requestReferentialisation(double requestTime, IChunk referencePotentialChunk);
	
	public void specify(IChunk featureReceiver, IChunk featureProvider,
			double startTime, double endTime, IChunk requestChunk);
	
	public void instantiate(IChunk typeSchema, double startTime, double endTime, IChunk requestChunk);
	
	public void linkArgumentToReferentOfCallableInvocation(String roleIn, IChunk callableInvocation,
			IChunk callableInvocationReferent, String roleId, IChunk oldArgument, IChunk newArgument,
			double startTime, double endTime, IChunk requestChunk);
	
	public void linkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
			IChunk referencePotential, IChunk referent, String declaredIn,
			ISlot roleContainer, String roleId, IChunk role, String argumentReferencePotentialId,
			IChunk argumentReferencePotential, IChunk argumentReferent,
			double startTime, double endTime, IChunk requestChunk);
	
	/**
	 * The TWM module adds an access to the given chunk.
	 * 
	 * @param kindOfChunk
	 * @param chunk
	 * @param startTime
	 * @param endTime
	 * @param requestChunk
	 */
	public void access(String kindOfChunk, IChunk chunk,
			double startTime, double endTime, IChunk requestChunk);
	
}
