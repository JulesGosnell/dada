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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Methods are not Serializable !
// TODO: tidy up

@Deprecated
public class IntrospectiveMetadata<K, V> implements Metadata<K, V> {

	private static final String GET = "get";
	private static final int GET_LENGTH = GET.length();
	
	private final Class<?> clazz;
	private final Creator<V> creator;
	private final Collection<Object> keyAttributeKeys;
	private final List<Class<?>> attributeTypes;
	private final List<Object> attributeKeys;
	private final List<Getter<?, V>> attributeGetters;
	private final int keyIndex;
	private final Map<String, Class<?>> nameToType;
	private final Map<String, Getter<?, V>> nameToGetter;

	public IntrospectiveMetadata(Class<?> clazz,Creator<V> creator, Object key) throws NoSuchMethodException {
		this.clazz = clazz;
		this.creator = creator;
		this.keyAttributeKeys = Collections.singleton(key);
		attributeTypes= new ArrayList<Class<?>>();
		attributeKeys = new ArrayList<Object>();
		attributeGetters = new ArrayList<Getter<?,V>>();
		nameToType = new HashMap<String, Class<?>>();
		nameToGetter = new HashMap<String, Getter<?,V>>();
		int i = 0;
		for (Method method : clazz.getMethods()) {
			final int index = i++;
			// is it a getter ?
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (!method.getDeclaringClass().equals(Object.class) && methodName.startsWith(GET) && parameterTypes.length == 0) {
				String attributeName = methodName.substring(GET_LENGTH);
				Class<?> type = method.getReturnType();
				attributeTypes.add(type);
				attributeKeys.add(attributeName);
				Getter<Object, V> getter = new Getter<Object, V>() {
					@Override
					public Object get(V value) {
						return getAttributeValue(value, index);
					}
				};
				attributeGetters.add(getter);
				nameToType.put(attributeName, type);
				nameToGetter.put(attributeName, getter);
			}
		}
		keyIndex = attributeKeys.indexOf(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getAttributeValue(V value, int index) {
		try {
			//Method method = getters.get(index);
			Method method = clazz.getMethod(GET + attributeKeys.get(index), (Class<?>[]) null);
			return (V) method.invoke(value, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Class<?>> getAttributeTypes() {
		return attributeTypes;
	}

	@Override
	public List<Object> getAttributeKeys() {
		return attributeKeys;
	}

	@Override
	public K getKey(V value) {
		return (K) getAttributeValue(value, keyIndex);
	}

	@Override
	public List<Getter<?, V>> getAttributeGetters() {
		return attributeGetters;
	}

	@Override
	public Collection<Object> getKeyAttributeKeys() {
		return keyAttributeKeys;
	}

	@Override
	public V create(Object... args) {
		return creator.create(args);
	}

	@Override
	public Creator<V> getCreator() {
		return creator;
	}

	@Override
	public V create(Collection<Object> args) {
		return creator.create(args.toArray());
	}

	@Override
	public Class<?> getAttributeType(Object key) {
		return nameToType.get(key);
	}

	@Override
	public Getter<?, V> getAttributeGetter(Object key) {
		return nameToGetter.get(key);
	}

}
