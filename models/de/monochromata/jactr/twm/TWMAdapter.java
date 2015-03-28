package de.monochromata.jactr.twm;

import org.jactr.core.chunk.IChunk;
import org.jactr.core.slot.ISlot;

public class TWMAdapter implements ITWMListener {

	@Override
	public void requestWordActivation(double startTime, IChunk wordChunk) {
	}

	@Override
	public void requestReferentialisation(double startTime, IChunk referencePotentialChunk) {
	}

	@Override
	public void specify(IChunk featureReceiver, IChunk featureProvider,
			double startTime, double endTime, IChunk requestChunk) {
	}

	@Override
	public void instantiate(IChunk typeSchema, double startTime,
			double endTime, IChunk requestChunk) {
	}

	@Override
	public void linkArgumentToReferentOfCallableInvocation(String roleIn,
			IChunk callableInvocation, IChunk callableInvocationReferent,
			String roleId, IChunk oldArgument, IChunk newArgument,
			double startTime, double endTime, IChunk requestChunk) {
	}

	@Override
	public void linkReferentOfCallableInvocationToArgumentsAndUpdateRoleIds(
			IChunk referencePotential, IChunk referent, String declaredIn,
			ISlot roleContainer, String roleId, IChunk role,
			String argumentReferencePotentialId,
			IChunk argumentReferencePotential, IChunk argumentReferent,
			double startTime, double endTime, IChunk requestChunk) {
	}

	@Override
	public void access(String kindOfChunk, IChunk chunk, double startTime,
			double endTime, IChunk requestChunk) {
	}

}
