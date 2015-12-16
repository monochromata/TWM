package de.monochromata.jactr.dm;

import org.jactr.io.participant.modules.DeclarativeModuleParticipant;

public class NonMergingDeclarativeModuleParticipant extends DeclarativeModuleParticipant {

	public NonMergingDeclarativeModuleParticipant() {
		super();
		setInstallableClass(NonMergingDeclarativeModule6.class);
	}

}
