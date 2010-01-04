/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// TODO: why are method signatures in terms of Object and not T ?

/**
 * A Set<T> which reference counts elements in and out - i.e. an element must be removed as many times as it
 * has been inserted before it will actually be removed.
 * 
 * @author jules
 *
 * @param <T>
 */
public class ReferenceCountedSet<T> implements Set<T> {

	private static Integer ZERO = 0;
	
	private Map<T, Integer> map = new HashMap<T, Integer>();
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public Object[] toArray() {
		return map.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return map.keySet().toArray(a);
	}

	@Override
	public boolean add(T e) {
		synchronized (map) {
			Integer count = map.get(e);
			if (count == null) {
				map.put(e, ZERO);
				return true;
			} else {
				map.put(e, count + 1);
				return false;
			}
		}
	}

	@Override
	public boolean remove(Object o) {
		synchronized (map) {
			Integer count = map.get(o);
			if (count == null) {
				return false;
			} else {
				if (count.equals(ZERO)) {
					map.remove(o);
					return true;
				} else {
					map.put((T)o, count - 1);
					return false;
				}
			}
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return map.keySet().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		int count = 0;
		for (T t : c)
			if (add(t))
				count++;
		return count > 0;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		int count = 0;
		for (Object t : c)
			if (remove(t))
				count++;
		return count > 0;
	}

	@Override
	public void clear() {
		map.clear();
	}
	
}