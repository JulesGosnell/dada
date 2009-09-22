package org.omo.immutable;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public interface IList<E> {

	public E get(int index);

	public int indexOf(Object o);

	public int lastIndexOf(Object o);

	public ListIterator<E> listIterator();

	public ListIterator<E> listIterator(int index);

	public List<E> subList(int fromIndex, int toIndex);

	public void add(int index, E element);

	public boolean addAll(int index, Collection<? extends E> c);

	public E remove(int index);

	public E set(int index, E element);

}