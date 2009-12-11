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


import junit.framework.TestCase;


public class TableAbstractTestCase extends TestCase {

	public static class Getter implements Runnable {

		private final Integer key;
		private final Table<Integer, String> table;
		
		private String value;
		
		public Getter(Integer key, Table<Integer, String> table) {
			this.key = key;
			this.table = table;
		}
		
		@Override
		public void run() {
			value = table.get(key);
		}

		public String getValue() {
			return value;
		}
	}

	public void testTable(Table<Integer, String> table) {

		// factory failure...
		assertTrue(table.get(0) == null);

		// factory creation of unused key
		assertTrue(table.get(1).equals("1"));

		// put and get of same reference
		String two = "2";
		table.put(2, two);
		assertTrue(table.get(2) == two);
		
		// overwrite existing value
		String two2 = "two";
		table.put(2, two2);
		assertTrue(table.get(2) == two2);
		
	}
}
