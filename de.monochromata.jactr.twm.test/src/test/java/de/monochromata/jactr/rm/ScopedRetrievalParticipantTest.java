package de.monochromata.jactr.rm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jactr.io.participant.impl.BasicASTParticipant;
import org.junit.Test;

public class ScopedRetrievalParticipantTest {

	@Test
	public void testGetParticipantClass() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Method method = BasicASTParticipant.class.getDeclaredMethod("getParticipantClass");
		method.setAccessible(true);
		assertThat(method.invoke(new ScopedRetrievalParticipant()), equalTo(ScopedRetrievalModule6.class));
	}

}
