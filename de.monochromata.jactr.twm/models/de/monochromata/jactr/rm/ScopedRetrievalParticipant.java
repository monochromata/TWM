package de.monochromata.jactr.rm;

import org.jactr.io.participant.modules.RetrievalModuleParticipant;

public class ScopedRetrievalParticipant
	extends RetrievalModuleParticipant {

	public ScopedRetrievalParticipant() {
		super();
		setInstallableClass(ScopedRetrievalModule6.class);
	}

}
