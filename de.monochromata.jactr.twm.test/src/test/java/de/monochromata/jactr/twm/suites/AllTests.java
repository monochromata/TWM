package de.monochromata.jactr.twm.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.monochromata.jactr.rm.ScopedRetrievalParticipantTest;
import de.monochromata.jactr.tls.ScopeTest;

/**
 * All tests for text-world models for jACT-R.
 * 
 * <p>
 * Needs to be run as a JUnit Plug-in Test, but not in the UI thread.
 * </p>
 * 
 * @author monochromata
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ ScopeTest.class, ScopedRetrievalParticipantTest.class, })
public class AllTests {

}
