package de.monochromata.jactr.rm;

import org.jactr.core.utils.IInstallable;
import org.jactr.io.participant.modules.RetrievalModuleParticipant;

public class ScopedRetrievalParticipant
	extends RetrievalModuleParticipant {

	public ScopedRetrievalParticipant() {
		super();
		setInstallableClass(ScopedRetrievalModule6.class);
	}

	// Overriding method to make it public to permit testing.
	@Override
	public Class<? extends IInstallable> getParticipantClass() {
		return super.getParticipantClass();
	}

}
