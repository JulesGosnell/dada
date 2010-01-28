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
import java.util.List;

// TODO: Methods are not Serializable !
// TODO: tidy up

public class IntrospectiveMetadata<K, V> implements Metadata<K, V> {

	private static final String GET = "get";
	private static final int GET_LENGTH = GET.length();
	
	private final Class<?> clazz;
	private final List<Class<?>> attributeTypes;
	private final List<String> attributeNames;
	private final List<Getter<?, V>> attributeGetters;
	private final int keyIndex;
	//private final List<Method> getters;

	public IntrospectiveMetadata(Class<?> clazz, String keyName) throws NoSuchMethodException {
		this.clazz = clazz;
		attributeTypes= new ArrayList<Class<?>>();
		attributeNames = new ArrayList<String>();
		attributeGetters = new ArrayList<Getter<?,V>>();
		//getters = new ArrayList<Method>();
		int i = 0;
		for (Method method : clazz.getMethods()) {
			final int index = i++;
			// is it a getter ?
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (!method.getDeclaringClass().equals(Object.class) && methodName.startsWith(GET) && parameterTypes.length == 0) {
				String attributeName = methodName.substring(GET_LENGTH);
				attributeTypes.add(method.getReturnType());
				attributeNames.add(attributeName);
				attributeGetters.add(new Getter<Object, V>() {
					@Override
					public Object get(V value) {
						return getAttributeValue(value, index);
					}
				});
				//getters.add(method);
			}
		}
		keyIndex = attributeNames.indexOf(keyName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getAttributeValue(V value, int index) {
		try {
			//Method method = getters.get(index);
			Method method = clazz.getMethod(GET + attributeNames.get(index), (Class<?>[]) null);
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
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public K getKey(V value) {
		return (K) getAttributeValue(value, keyIndex);
	}

	@Override
	public Class<?> getValueClass() {
		return clazz;
	}

	@Override
	public List<Getter<?, V>> getAttributeGetters() {
		return attributeGetters;
	}

}
