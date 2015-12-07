package de.monochromata.jactr.tls;

import java.util.Collection;

import org.jactr.core.slot.IMutableSlot;

/**
 * A slot that can contain more than one value at once.
 */
public interface ICollectionSlot extends IMutableSlot {

	/**
	 * Implementation of this method should always throw
	 * {@link UnsupportedOperationException}.
	 * {@link #getValues()} should be used instead.
	 */
	@Override
	public Object getValue() throws UnsupportedOperationException;
	
	public void setValues(Collection<Object> values);

	public Collection<Object> getValues();
	
	public void add(Object value);
	
	public void remove(Object value);

	public void addAll(Collection<Object> values);

	public void removeAll(Collection<Object> values);
	
	/**
	 * Returns true, if all elements from the given collection
	 * are contained in the collection of values of this slot
	 * as per {@link #containsElement(Object)}. I.e. this
	 * collection might contain further elements besides those
	 * from the given collection.
	 * 
	 * @param other
	 * @return
	 */
	public boolean contains(Collection<Object> other);

	/**
	 * Returns true, if the collection slot contains a variable value
	 * as per {@link #isVariableValue()}, or given object is a string and the
	 * collection of values in this collection slot contains
	 * an equal string (ignoring case) or if the given object
	 * is not a string but there is an equal object in the
	 * collection of slot values as per {@link Object#equals(Object)}.
	 * 
	 * @param element
	 * @return
	 * @see String#equalsIgnoreCase(String)
	 */
	public boolean containsElement(Object element);
	
	/**
	 * Returns a collection of the elements in this slot that are not
	 * contained in the given other slot. Objects are compared by means
	 * of {@link Object#equals(Object)} in general, only objects that
	 * implement {@link IChunk} are compared via
	 * {@link IChunk#equalsSymbolic(IChunk)} for their symbolic chunk.
	 *  
	 * @param other
	 * @return
	 */
	public Collection<Object> difference(ICollectionSlot other);

}