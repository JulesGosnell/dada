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
package org.dada.asm;

import junit.framework.TestCase;

public class ClassFactoryTestCase extends TestCase {
	
	private static class TestClassLoader extends ClassLoader {
		protected Class<?> defineClass(String name, byte[] bytes) {
			return defineClass(bytes, 0, bytes.length);
		}
	}
	
	public void test() throws Exception {
		ClassFactory factory = new ClassFactory();
		String canonicalClassName = "org.dada.test.ImmutableDatum";
		
		String[][] fields = new String[][] {
				{"byte",    "field00"},
				{"short",   "field01"},
				{"int",     "field02"},
				{"long",    "field03"},
				{"float",   "field04"},
				{"double",  "field05"},
				{"boolean", "field06"},
				{"char",    "field07"},
			};
		
		byte[] bytes = factory.create(canonicalClassName, fields);
		TestClassLoader loader = new TestClassLoader();
		Class<?> type = loader.defineClass(canonicalClassName, bytes);
		assertTrue(type.getCanonicalName().equals(canonicalClassName));
		Class<?>[] parameterTypes = {
				Byte.TYPE,
				Short.TYPE,
				Integer.TYPE,
				Long.TYPE,
				Float.TYPE,
				Double.TYPE,
				Boolean.TYPE,
				Character.TYPE,
		};
		Object[] initArgs = {
				Byte.MAX_VALUE,
				Short.MAX_VALUE,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				Float.MAX_VALUE,
				Double.MAX_VALUE,
				Boolean.TRUE,
				Character.MAX_VALUE,
		};
		Object instance = type.getConstructor(parameterTypes).newInstance(initArgs);
		
		type.getMethod("getField00", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Byte.MAX_VALUE);
		type.getMethod("getField01", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Short.MAX_VALUE);
		type.getMethod("getField02", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Integer.MAX_VALUE);
		type.getMethod("getField03", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Long.MAX_VALUE);
		type.getMethod("getField04", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Float.MAX_VALUE);
		type.getMethod("getField05", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Double.MAX_VALUE);
		type.getMethod("getField06", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Boolean.TRUE);
		type.getMethod("getField07", (Class<?>[]) null).invoke(instance, (Object[]) null).equals(Character.MAX_VALUE);
	}

}
