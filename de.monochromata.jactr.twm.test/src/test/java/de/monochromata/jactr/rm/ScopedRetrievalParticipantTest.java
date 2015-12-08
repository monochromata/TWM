package de.monochromata.jactr.rm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ScopedRetrievalParticipantTest {

	@Test
	public void testGetParticipantClass() {
		assertThat(new ScopedRetrievalParticipant().getParticipantClass(), equalTo(ScopedRetrievalModule6.class));
	}

}
