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
import java.util.List;

// TODO: Methods are not Serializable !
// TODO: tidy up

@Deprecated
public class IntrospectiveMetadata<K, V> implements Metadata<K, V> {

	private static final String GET = "get";
	private static final int GET_LENGTH = GET.length();
	
	private final Class<?> clazz;
	private final Creator<K> keyCreator;
	private final Creator<V> creator;
	private final Collection<Object> keyAttributeKeys;
	private final List<Object> attributeKeys;
	private final int keyIndex;

	public IntrospectiveMetadata(Class<?> clazz, Creator<K> keyCreator, Creator<V> creator, Object key) throws NoSuchMethodException {
		this.clazz = clazz;
		this.keyCreator = keyCreator;
		this.creator = creator;
		this.keyAttributeKeys = Collections.singleton(key);
		attributeKeys = new ArrayList<Object>();
		for (Method method : clazz.getMethods()) {
			// is it a getter ?
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (!method.getDeclaringClass().equals(Object.class) && methodName.startsWith(GET) && parameterTypes.length == 0) {
				String attributeName = methodName.substring(GET_LENGTH);
				attributeKeys.add(attributeName);
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
	public Creator<K> getKeyCreator() {
		return keyCreator;
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
	public Getter<K, V> getPrimaryGetter() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public List<Attribute<Object, V>> getAttributes() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Collection<Object> getPrimaryKeys() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Collection<Object> getVersionKeys() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public org.dada.core.Metadata.VersionComparator<V> getVersionComparator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Getter<K, V> getVersionGetter() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

}
