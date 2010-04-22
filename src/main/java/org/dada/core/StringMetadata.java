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
import java.util.Collections;
import java.util.List;

@Deprecated
public class StringMetadata implements Metadata<String, String> {

	private final Collection<Object> keyAttributeKeys;
	private final List<Class<?>> attributeTypes;
	private final List<Object> attributeKeys;
	private final List<Getter<?, String>> attributeGetters;
	private final Creator<String> creator;

	public StringMetadata(Object key) {
		keyAttributeKeys = Collections.singleton(key); 
		attributeTypes = new ArrayList<Class<?>>();
		attributeTypes.add(String.class);
		attributeKeys = Collections.singletonList(key);
		attributeGetters = new ArrayList<Getter<?,String>>();
		attributeGetters.add(new Getter<Object, String>() {@Override public Object get(String value) {return value;}});
		creator = new Creator<String>() {@Override public String create(Object... args) {return (String)args[0];}};
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
	public Object getAttributeValue(String value, int index) {
		return value;
	}

	@Override
	public String getKey(String value) {
		return value;
	}

	@Override
	public List<Getter<?, String>> getAttributeGetters() {
		return attributeGetters;
	}

	@Override
	public Collection<Object> getKeyAttributeKeys() {
		return keyAttributeKeys;
	}

	@Override
	public String create(Object... args) {
		return (String)args[0];
	}

	@Override
	public Creator<String> getCreator() {
		return creator;
	}

	@Override
	public String create(Collection<Object> args) {
		return creator.create(args.toArray());
	}

	@Override
	public Class<?> getAttributeType(Object key) {
		return attributeTypes.get(0);
	}

	@Override
	public Getter<?, String> getAttributeGetter(Object key) {
		return attributeGetters.get(0);
	}
}
