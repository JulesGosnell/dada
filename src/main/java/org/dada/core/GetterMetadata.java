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
import java.util.List;

public class GetterMetadata<K, V> implements Metadata<K, V> {

	private final Class<?> valueClass;
	private final List<Class<?>> attributeTypes;
	private final List<String> attributeNames;
	private final Getter<K, V> keyGetter;
	private final Getter<Object, Object>[] getters;

	public GetterMetadata(Class<?> valueClass, Collection<Class<?>> attributeTypes, Collection<String> attributeNames, Collection<Getter<?, ?>> getters) {
		this.valueClass = valueClass;
		this.attributeTypes = new ArrayList<Class<?>>(attributeTypes);
		this.attributeNames = new ArrayList<String>(attributeNames);
		this.getters = getters.toArray(new Getter[getters.size()]);
		this.keyGetter = (Getter<K, V>)this.getters[0];
	}

	@Override
	public List<Class<?>> getAttributeTypes() {
		return attributeTypes;
	}

	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
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
	public Class<?> getValueClass() {
		return valueClass;
	}

}
