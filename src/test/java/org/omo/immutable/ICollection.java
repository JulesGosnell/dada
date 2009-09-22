package org.omo.immutable;

import java.util.Collection;
import java.util.Iterator;

public interface ICollection<E> {

	public boolean contains(Object o);

	public boolean containsAll(Collection<?> c);

	public boolean isEmpty();

	public Iterator<E> iterator();

	public int size();

	public Object[] toArray();

	public <T> T[] toArray(T[] a);

	public boolean add(E e);

	public boolean addAll(Collection<? extends E> c);

	public void clear();

	public boolean remove(Object o);

	public boolean removeAll(Collection<?> c);

	public boolean retainAll(Collection<?> c);

}