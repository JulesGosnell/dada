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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class IntrospectiveMetadata<K, V> implements Metadata<K, V> {

	private final Class<?> clazz;
	private final List<String> attributeNames;
	private int keyIndex;
	//private final List<Method> getters;

	public IntrospectiveMetadata(Class<?> clazz, String keyName) throws SecurityException, NoSuchMethodException {
		this.clazz = clazz;
		keyName = "get"+keyName;
		attributeNames = new ArrayList<String>();
		//getters = new ArrayList<Method>();
		for (Method method : clazz.getMethods()) {
			// is it a getter ?
			String name = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (name.startsWith("get") &&
				!method.getDeclaringClass().equals(Object.class) &&
				(parameterTypes==null || parameterTypes.length==0)) {
				if (name.equals(keyName))
					keyIndex = attributeNames.size();
				attributeNames.add(name.substring(3));
				//getters.add(method);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getAttributeValue(V value, int index) {
		try {
			//Method method = getters.get(index);
			Method method = clazz.getMethod("get"+attributeNames.get(index), (Class<?>[])null);
			return (V)method.invoke(value, (Object[])null);
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException)e: new RuntimeException(e);  
		}
	}

	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public K getKey(V value) {
		try {
			return (K)getAttributeValue(value, keyIndex);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
