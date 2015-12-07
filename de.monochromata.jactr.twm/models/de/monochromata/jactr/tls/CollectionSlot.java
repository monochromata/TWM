package de.monochromata.jactr.tls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.slot.DefaultMutableSlot;
import org.jactr.core.slot.IMutableSlot;
import org.jactr.core.slot.INotifyingSlotContainer;
import org.jactr.core.slot.ISlot;
import org.jactr.core.slot.NotifyingSlot;

/**
 * A slot that is able to contain more than one value
 * 
 * TODO: The ISlot interface should not be broken
 * TODO: Das muss dann aber auch bei der Berechnung der Aktivierung
 * berücksichtigt werden
 */
public class CollectionSlot implements Comparable<ISlot>, ISlot, IMutableSlot, ICollectionSlot {

	/**
	 * Logger definition
	 */
	private static final transient Log LOGGER = LogFactory.getLog(CollectionSlot.class);
	
	private String toString;
	
	private String name;
	private ArrayList<Object> values = new ArrayList<Object>();
	private INotifyingSlotContainer container;
	
	public CollectionSlot(String name) {
		this.name = name;
	}

	/**
	 * Creates a new Collection slot with the given name and value(s).
	 * If the given value is an instance of {@link Collection}, all
	 * elements of the collection are added to this slot, not the
	 * collection itself.
	 *  
	 * @param name
	 * @param value An object or a {@link Collection}
	 */
	@SuppressWarnings("unchecked")
	public CollectionSlot(String name, Object value) {
		this(name);
		if(value != null) {
			if(value instanceof Collection) {
				this.values.addAll((Collection<Object>)value);
			} else {
				this.values.add(value);
			}
		}
	}

	public CollectionSlot(String name, Object value,
			INotifyingSlotContainer container) {
		this(name, value);
		this.container = container;
	}
	
	public CollectionSlot(String name, Collection<Object> values) {
		this(name, (Object)values);
	}
	
	public CollectionSlot(CollectionSlot slot) {
		this(slot.getName(), slot.getValues(), slot.getContainer());
	}
	
	public CollectionSlot(ISlot slot, INotifyingSlotContainer container) {
		this(slot.getName(), slot.getValue(), container);
	}

	public INotifyingSlotContainer getContainer() {
		return container;
	}

	public void setContainer(INotifyingSlotContainer container) {
		this.container = container;
	}

	// Implementation of Comparable

	/**
	 * Like in {@link BasicSlot}, the comparison is based on the name only.
	 */
	@Override
	public int compareTo(ISlot other) {
		return getName().compareTo(other.getName());
	}
	
	// Implementation of IMutableSlot
	
	/**
	 * Clears the collection of values held by the slot and adds the
	 * given value. If the given value is a {@link Collection}, the
	 * elements of the collection will be added.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object value) {
		if(value == null) {
			throw new NullPointerException();
		} else if(value instanceof Collection) {
			setValues((Collection<Object>)value);
		} else { 
			changeValues(() -> {
				this.values = new ArrayList<Object>(1);
				this.values.add(value);
				return this.values;
			});
		}
	}
	
	// Implementation of ICollectionSlot
	
	/* (non-Javadoc)
	 * @see de.monochromata.jactr.tls.ICollectionSlot#setValues(java.util.Collection)
	 */
	@Override
	public void setValues(Collection<Object> values) {
		changeValues(() -> {
			this.values = new ArrayList<Object>(values);
			return this.values;
		});
	}
	
	/* (non-Javadoc)
	 * @see de.monochromata.jactr.tls.ICollectionSlot#add(java.lang.Object)
	 */
	@Override
	public void add(Object value) {
		changeValues(() -> {
			ArrayList<Object>newValues = new ArrayList<Object>(this.values.size()+1);
			newValues.addAll(this.values);
			newValues.add(value);
			this.values = newValues;
			return this.values;
		});
	}
	
	/* (non-Javadoc)
	 * @see de.monochromata.jactr.tls.ICollectionSlot#addAll(java.util.Collection)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void addAll(Collection<Object> values) {
		changeValues(() -> {
			this.values = (ArrayList<Object>)this.values.clone();
			this.values.addAll(values);
			return this.values;
		});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void remove(Object value) {
		changeValues(() -> {
			this.values = (ArrayList<Object>)this.values.clone();
			this.values.remove(value);
			return this.values;
		});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void removeAll(Collection<Object> values) {
		changeValues(() -> {
			this.values = (ArrayList<Object>)this.values.clone();
			this.values.remove(values);
			return this.values;
		});
	}
	
	private void changeValues(Supplier<ArrayList<Object>> newValuesSupplier) {
		ArrayList<Object> oldValues = this.values;
		clearToString();
		ArrayList<Object> newValues = newValuesSupplier.get();
		if(container != null) {
			try {
				container.valueChanged(this, oldValues, newValues);
			} catch (Exception e) {
		        // roll back
		        if (LOGGER.isWarnEnabled())
		          LOGGER.warn(String.format(
                      "Change of %s=%s to %s resulted in an exception (%s), rolling back",
                      getName(), oldValues, newValues, e.getMessage()), e);
				this.values = oldValues;
			}
		}
	}
	
	// Implementation of ISlot

	@Override
	public Collection<Object> difference(ICollectionSlot other) {
		Collection<Object> otherValues = other.getValues();
		List<Object> difference = new LinkedList<Object>();
		for(Object value: values) {
			if(value instanceof IChunk) {
				// Chunks are compared by chunk type and slot values
				IChunk chunk = (IChunk)value;
				boolean chunkFoundInOther = false;
				for(Object otherValue: otherValues) {
					if(otherValue instanceof IChunk
						&& ((IChunk)otherValue).equalsSymbolic(chunk)) {
						chunkFoundInOther = true;
						break;
					}
				}
				if(!chunkFoundInOther)
					difference.add(value);
			} else if(!otherValues.contains(value)) {
				// Non-chunk values are compared via Object.equals(Object)
				difference.add(value);
			}
		}
		return difference;
	}

	@Override
	public boolean isVariable() {
		return isVariableValue();
	}

	/**
	 * Returns true, if there is a variable (a string prefixed by =,
	 * containing no spaces) in the collection of values held by this
	 * CollectionSlot.
	 * 
	 * @see BasicSlot#isVariableValue()
	 */
	@Override
	public boolean isVariableValue() {
		for(Object value: values) {
			if(value instanceof String) {
				String str = (String)value;
				if(str.startsWith("=") && str.indexOf(' ') == -1)
					return true;
			}
		}
		return false;
	}
	
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Throws UnsupportedOperationException except for a number of
	 * invoking method granted "permission" via stack inspection.
	 * 
	 * Note that {@link #getValues()} should be invoked instead.
	 * 
	 * TODO: Remove strack trace inspection
	 * 
	 * @return
	 * @see #getValues()
	 */
	@Override
	public Object getValue() {
		try {
			throw new UnsupportedOperationException();
		} catch(UnsupportedOperationException e) {
			StackTraceElement[] stackTrace = e.getStackTrace();
			if(stackTrace != null && stackTrace.length >= 2) {
				String callerName = stackTrace[1].getClassName();
				if(callerName.equals("org.jactr.io.resolver.ASTResolver")
					|| callerName.equals("de.monochromata.jactr.twm.Referentialisation")
					|| callerName.equals("de.monochromata.jactr.instruments.activation.ChunkExporter")) {
					if(values.isEmpty()) {
						return null;
					} else if(values.size() == 1) {
						return values.get(0);
					} else {
						return values;
					}
				}
			}
			// Re-throw, if no "permission" was granted
			throw e;
		}
	}
	
	/**
	 * Always returns an unmodifiable collection of values.
	 * The {@link Collection} might be empty.
	 * 
	 * @see Collection
	 */
	@Override
	public Collection<Object> getValues() {
		return Collections.unmodifiableList(values);
	}

	/**
	 * TODO: Document semantics
	 */
	@Override
	public boolean equalValues(Object other) {
		if(other == null)
			return values.isEmpty();
		if(isVariableValue()) // && other != null is implied
			return true;
		
		// Get values to compare
		Collection<Object> otherValues = null;
		if(other instanceof ICollectionSlot) {
			otherValues = ((ICollectionSlot)other).getValues();
		} else if(other instanceof ISlot) {
			otherValues = Collections.singleton(((ISlot)other).getValue());
		} else {
			otherValues = Collections.singleton(other);
		}

		// Compare all values
		for(Object value: values) {
			boolean valueIsString = value instanceof String;
			boolean valueFound = false;
			for(Object otherValue: otherValues) {
				boolean otherValueIsString = otherValue instanceof String;
				if(valueIsString && otherValueIsString) {
					// Compare strings
					if(((String)value).equalsIgnoreCase((String)otherValue)) {
						valueFound = true;
						break;
					}
				} else if(!valueIsString && !otherValueIsString) {
					// Compare non-strings
					if(value.equals(otherValue)) {
						valueFound = true;
						break;
					}
				} // no else: do not mix strings with non-strings during comparison
			}
			if(!valueFound) {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.monochromata.jactr.tls.ICollectionSlot#contains(java.util.Collection)
	 */
	@Override
	public boolean contains(Collection<Object> other) {
		if(other == null)
			throw new NullPointerException();
		
		for(Object element: other) {
			if(!containsElement(element))
				return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.monochromata.jactr.tls.ICollectionSlot#containsElement(java.lang.Object)
	 */
	@Override
	public boolean containsElement(Object element) {
		if(element == null)
			throw new NullPointerException();
		if(isVariableValue()) // && element == null is implied
			return true;
		for(Object value: values) {
			if(element instanceof String
				&& value instanceof String
				&& ((String)value).equalsIgnoreCase((String)element)) {
				return true;
			} else if(value.equals(element)) {
				return true;
			}
		}
		return false;
	}
	
	// Implementation of Object

	@Override
	public ISlot clone() {
		return new CollectionSlot(this);
	}
	
	/**
	 * Like in the case of {@link DefaultMutableSlot}, the hash code includes
	 * the name only to remain constant when values change.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CollectionSlot other = (CollectionSlot) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	protected synchronized void clearToString() {
		toString = null;
	}
	
	protected synchronized String createToString() {
		return String.format("%1$s(%2$s)", getName(), getValues());
	}
	
	@Override
	public synchronized String toString() {
		if(toString == null)
			toString= createToString();
		return toString;
	}
	
}
