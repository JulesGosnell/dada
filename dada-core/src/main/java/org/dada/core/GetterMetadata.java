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
import java.util.List;

public class GetterMetadata<K, V> implements Metadata<K, V> {

	private final Creator<V> creator;
	private final Collection<Object> keyAttributeKeys;
	private final Getter<K, V> keyGetter;
	private final Getter<Object, V>[] getters; // TODO: revisit relationship between attributeGetters and getters

	public GetterMetadata(Creator<V> creator, Collection<Object> keyAttributeKeys, Collection<Class<?>> attributeTypes, Collection<Object> attributeKeys, Collection<Getter<?, V>> getters) {
		this.creator = creator;
		this.keyAttributeKeys = keyAttributeKeys;
		this.getters = getters.toArray(new Getter[getters.size()]);
		this.keyGetter = (Getter<K, V>)this.getters[0];
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
	public Collection<Object> getKeyAttributeKeys() {
		return keyAttributeKeys;
	}

	@Override
	public Creator<V> getCreator() {
		return creator;
	}

	@Override
	public Attribute<Object, V> getAttribute(Object key) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Getter<K, V> getKeyGetter() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public List<Attribute<Object, V>> getAttributes() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

}
