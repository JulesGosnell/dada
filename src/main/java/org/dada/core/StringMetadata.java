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


public class StringMetadata implements Metadata<String, String> {

	private final Collection<String> keyAttributeNames;
	private final List<Class<?>> attributeTypes;
	private final List<String> attributeNames;
	private final List<Getter<?, String>> attributeGetters;

	public StringMetadata(String keyName) {
		keyAttributeNames = Collections.singleton(keyName); 
		attributeTypes = new ArrayList<Class<?>>();
		attributeTypes.add(String.class);
		attributeNames = Collections.singletonList(keyName);
		attributeGetters = new ArrayList<Getter<?,String>>();
		attributeGetters.add(new Getter<Object, String>() {@Override public Object get(String value) {return value;}});
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
	public Object getAttributeValue(String value, int index) {
		return value;
	}

	@Override
	public String getKey(String value) {
		return value;
	}

	@Override
	public Class<?> getValueClass() {
		return String.class;
	}

	@Override
	public List<Getter<?, String>> getAttributeGetters() {
		return attributeGetters;
	}

	@Override
	public Collection<String> getKeyAttributeNames() {
		return keyAttributeNames;
	}
}
