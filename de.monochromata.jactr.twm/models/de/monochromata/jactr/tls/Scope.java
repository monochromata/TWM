package de.monochromata.jactr.tls;

/**
 * A scope limits the context from which a chunk can be
 * accessed. E.g. local variables declared in different
 * method declarations will have incompatible scopes so
 * that a request for a chunk in method declaration A
 * will not return a chunk declared in method declaration B.
 * 
 * @see #isIdenticalOrEncloses(Scope)
 */
public class Scope {
	
	/**
	 * The global scope encloses all other scopes and has an empty specifier.
	 * 
	 * @see #isIdenticalOrEncloses(Scope)
	 */
	public static final Scope GLOBAL = new Scope("");
	
	private final String specifier;
	
	/**
	 * Use {@link #create(String)} instead.
	 */
	private Scope(String specifier) {
		this.specifier = specifier;
	}
	
	/**
	 * Create a new scope or return {@link #GLOBAL} scope.
	 * 
	 * @param specifier The specifier for the scope, may be empty or {@code null}.
	 * @return A new scope, or {@link #GLOBAL}, if the given specifier is {@code null} or empty.
	 */
	public static Scope create(String specifier) {
		if(specifier == null || specifier.length() == 0) {
			return GLOBAL;
		} else {
			return new Scope(specifier);
		}
	}
	
	/**
	 * Returns true, if the scope on which the method is
	 * invoked has a specifier that is a prefix of the
	 * specifier of the given other scope by means of
	 * {@link String#startsWith(String)}.
	 * 
	 * @param other
	 * @return
	 */
	public boolean isIdenticalOrEncloses(Scope other) {
		return other.specifier.startsWith(specifier);
	}
}
