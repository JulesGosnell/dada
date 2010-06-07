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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataImpl<K extends Comparable<K>, V> implements Metadata<K, V> {

	private final Creator<V> creator;
	private final List<Attribute<Object, V>> attributes;
	private final Getter<K, V> keyGetter;
	private final Map<Object, Attribute<Object, V>> keyToAttribute;
	private final Map<Object, Getter<?, V>> keyToGetter;

	private final Getter<?, V>[] getters; // for fast lookup
	
	public MetadataImpl(Creator<V> creator, Collection<Object> keys, Collection<Attribute<Object, V>> attributes) {
		this.creator = creator;
		this.attributes = new ArrayList<Attribute<Object,V>>(attributes);
		int size = attributes.size();
		final List<Getter<?, V>> attributeGetters = new ArrayList<Getter<?, V>>(size);
		keyToGetter = new HashMap<Object, Getter<?, V>>(size);
		keyToAttribute = new HashMap<Object, Attribute<Object, V>>(size);

		for (Attribute<Object, V> attribute : attributes) {
			Object key = attribute.getKey();
			Getter<Object, V> getter = attribute.getGetter();
			keyToAttribute.put(key, attribute);
			keyToGetter.put(key, getter);
			attributeGetters.add(getter);
		}
		this.getters = attributeGetters.toArray(new Getter[size]);

		if (keys.size() == 1) {
			keyGetter = (Getter<K, V>)keyToGetter.get(keys.iterator().next());
		} else {
			final Getter<Comparable<K>, V>[] getters = new Getter[keys.size()];
			int i = 0;
			for (Object key : keys)
				getters[i++] = (Getter<Comparable<K>, V>)keyToGetter.get(key);
			keyGetter = new Getter<K, V>() {
				@Override
				public K get(V value) {
					Comparable<K>[] args = new Comparable[getters.length];
					int i = 0;
					for (Getter<Comparable<K>, V> getter : getters)
						args[i++] = getter.get(value);
					return (K)new Tuple(args);
				}
			};
		}
}


	@Override
	public Object getAttributeValue(V value, int index) {
		return getters[index].get(value);
	}

	@Override
	public K getKey(V value) {
		return keyGetter.get(value);
	}

	@Override
	public Getter<K, V> getKeyGetter() {
		return keyGetter;
	}

	@Override
	public Collection<Object> getKeyAttributeKeys() {
		throw new UnsupportedOperationException("deprecated method");
	}

	@Override
	public Creator<V> getCreator() {
		return creator;
	}

	@Override
	public Attribute<Object, V> getAttribute(Object key) {
		return keyToAttribute.get(key);
	}

	@Override
	public List<Attribute<Object, V>> getAttributes() {
		return attributes;
	}

}
