package de.monochromata.jactr.tls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.module.declarative.search.map.AbstractTypeValueMap;
import org.jactr.core.module.declarative.search.map.DefaultValueMap;
import org.jactr.core.module.declarative.search.map.IValueMap;

/**
 * A type value map that uses set semantics for collections of elements.
 * 
 * TODO: Use delegate type value maps for numbers etc. or implements
 * comparison semantics for numbers etc.?
 * TODO: It might also make sense to use a {@link SortedValueMap}
 * underneath based on set semantics.
 * 
 * @param <I>
 */
public class CollectionTypeValueMap<I> extends AbstractTypeValueMap<Object, I>{

	private static final Log LOGGER = LogFactory.getLog(CollectionTypeValueMap.class);
	
	private DefaultValueMap<Object, I> _valueMap = new DefaultValueMap<>();

	@Override
	public boolean isValueRelevant(Object value) {
		return true;
	}

	@Override
	public IValueMap<Object, I> getValueMap() {
		return _valueMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object asKeyType(Object value) {
		return value;
	}

	@SuppressWarnings("unchecked")
	protected void applyToCollection(Object value, Consumer<Object> consumer) {
		if(value instanceof Collection) {
			for(Object element: (Collection<Object>)value) {
				consumer.accept(element);
			}
		} else {
			consumer.accept(value);
		}
	}
	
	protected void applyToCollection(Object value, I indexable,
			BiConsumer<Object,I> consumer) {
		applyToCollection(value, v -> {
			consumer.accept(v, indexable);
		});
	}
	
	/**
	 * If the given value is a {@link Collection}, all elements of the collection are added.
	 */
	@Override
	public void add(Object value, I indexable) {
		applyToCollection(value, indexable, super::add);
	}

	/**
	 * If the given value is a {@link Collection}, all elements of the collection are removed.
	 */
	@Override
	public void remove(Object value, I indexable) {
		applyToCollection(value, indexable, super::remove);
	}

	/**
	 * If the given value is a {@link Collection}, the indexables for all elements
	 * of the collection will be removed.
	 */
	@Override
	public void clear(Object value) {
		applyToCollection(value, super::clear);
	}

	/**
	 * Returns a collection of all chunks that contain the given value.
	 * If the value is a collection, all chunks containing all elements
	 * of the collection are returned.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Collection<I> equalTo(Object value) {
		if(value instanceof Collection) {
			Set<I> results = null;
			// TODO: There might be a more efficient algorithm
			for(Object element: (Collection<Object>)value) {
				if(results == null) {
					results = new HashSet<I>(super.equalTo(element));
				} else {
					results.retainAll(super.equalTo(element));
				}
			}
			if(results == null)
				results = Collections.emptySet();
			return results;
		} else {
			return super.equalTo(value);
		}
	}

	/**
	 * Returns the number of chunks that contain the given value.
	 * If the value is a {@link Collection}, the sum of the numbers
	 * of chunks that contain the elements of the collection is returned.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public long equalToSize(Object value) {
		if(value instanceof Collection) {
			long size = 0;
			for(Object element: (Collection<Object>)value) {
				size += super.equalToSize(element);
			}
			return size;
		} else {
			return super.equalToSize(value);
		}
	}

	/**
	 * Returns all chunks that do not contain the given value.
	 * If the given value is a collection, all chunks are returned
	 * that contain neither of its elements.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Collection<I> not(Object value) {
		if(value instanceof Collection) {
			Collection<I> result = super.all();
			for(Object element: (Collection<Object>)value) {
				result.removeAll(super.equalTo(element));
			}
			return result;
		} else {
			return super.not(value);
		}
	}

	@Override
	public long notSize(Object value) {
		if(value instanceof Collection) {
			// A quick quess that should be about right for large numbers of
			// chunks with different values.
			return super.allSize();
		} else {
			return super.notSize(value);
		}
	}

	/**
	 * @throws UnsupportedOperationException This operation is not supported.
	 * 	Use {@link #equalTo(Object)} instead. 
	 */
	@Override
	public Collection<I> lessThan(Object value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException This operation is not supported.
	 * 	Use {@link #equalToSize(Object)} instead.
	 */
	@Override
	public long lessThanSize(Object value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException This operation is not supported.
	 * 	Use {@link #not(Object)} instead.
	 */
	@Override
	public Collection<I> greaterThan(Object value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException This operation is not supported.
	 * 	Use {@link #notSize(Object)} instead.
	 */
	@Override
	public long greaterThanSize(Object value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
