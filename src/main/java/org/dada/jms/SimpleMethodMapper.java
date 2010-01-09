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
package org.dada.jms;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SimpleMethodMapper implements MethodMapper<Integer> {

	protected /* final */ Class<?> interfaze;
	protected /* final */ transient List<Method> indexToMethod;
	protected /* final */ transient Map<Method, Integer> methodToIndex;


	public SimpleMethodMapper(Class<?> interfaze) {
		this.interfaze = interfaze;
		initialise();
	}

	@SuppressWarnings("unused") // deserialisation
	private SimpleMethodMapper() {
		// assumes interfaze is already set up...
		initialise();
	}

	public void initialise() {
		Map<String, Method> order = new TreeMap<String, Method>();
		for (Method method : interfaze.getMethods()) {
			order.put(makeKey(method), method);
		}

		indexToMethod = new ArrayList<Method>();
		methodToIndex = new HashMap<Method, Integer>();
		for (Method method : order.values()) {
			methodToIndex.put(method, indexToMethod.size());
			indexToMethod.add(method);
		}
	}

	protected String makeKey(Method method) {
		return method.getGenericReturnType().toString() + " " + method.getName() + "(" + arrayToString(method.getParameterTypes()) + ") " + arrayToString(method.getExceptionTypes());
	}

	protected String arrayToString(Object[] array) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if (i != 0)
				buffer.append(", ");
			buffer.append(array[i].toString());
		}
		return buffer.toString();
	}

	@Override
	public Integer getKey(Method method) {
		return methodToIndex.get(method);
	}

	@Override
	public Method getMethod(Integer key) {
		return indexToMethod.get(key);
	}

}
