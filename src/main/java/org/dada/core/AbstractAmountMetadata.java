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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAmountMetadata<K, V extends Datum<K>> implements Metadata<K, V> {

	private static List<String> makeStringList(String... array) {
		List<String> strings = new ArrayList<String>(array.length);
		for (String string : array)
			strings.add(string);
		return strings;
	}

	private final List<String> attributeNames;

	public AbstractAmountMetadata(String keyName, String amountName) {
		attributeNames = makeStringList(keyName, "Version", amountName);
	}

	@Override
	public List<String> getAttributeNames() {
		return attributeNames;
	}

	@Override
	public Object getAttributeValue(V value, int index) {
		switch (index) {
		case 0:
			return value.getId();
		case 1:
			return value.getVersion();
		case 2:
			return getAmount(value);
		default:
			return null;
		}
	}

	@Override
	public K getKey(V value) {
		return value.getId();
	}

	protected abstract BigDecimal getAmount(V value);
}
