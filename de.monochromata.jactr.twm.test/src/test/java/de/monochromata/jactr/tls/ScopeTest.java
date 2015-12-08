package de.monochromata.jactr.tls;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests {@link Scope}.
 * 
 * @author monochromata
 *
 */
public class ScopeTest {

	@Test
	public void createReturnsGlobalScopeForNullSpecifier() {
		assertThat(Scope.create(null), sameInstance(Scope.GLOBAL));
	}

	@Test
	public void createReturnsGlobalScopeForEmptySpecifier() {
		assertThat(Scope.create(""), sameInstance(Scope.GLOBAL));
	}

	@Test
	public void creatingANewScopeWithASpecifierTwiceDoesNotLeadToIdenticalInstancesButToIdenticalScopes() {
		Scope scope1 = Scope.create("specifier");
		Scope scope2 = Scope.create("specifier");

		assertThat(scope1, not(sameInstance(scope2)));
		assertTrue(scope1.isIdenticalOrEncloses(scope2));
		assertTrue(scope2.isIdenticalOrEncloses(scope1));
	}

	@Test
	public void createEnclosingScopes() {
		Scope enclosing = Scope.create("a");
		Scope enclosed = Scope.create("a.b");

		assertThat(enclosing, not(sameInstance(enclosed)));
		assertTrue(enclosing.isIdenticalOrEncloses(enclosed));
		assertFalse(enclosed.isIdenticalOrEncloses(enclosing));
	}

	// TODO: Add (equalsverifier and) hashCode and equals()?

}
