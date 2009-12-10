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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.dada.core.Table.Factory;

public class TableTestCase extends TestCase {

	public void testCompactOpenTable() {

		Factory<Integer, String> factory = new Factory<Integer, String>() {
			@Override
			public String create(Integer key) throws Exception {
				if (key == 0)
					throw new UnsupportedOperationException("NYI");
				else
					return "" + key;
			}
		};

		ArrayList<String> values = new ArrayList<String>();
		Table<Integer, String> table = new CompactOpenTable<String>(values, factory);

		testTable(table);
		
		// force a resize on put...
		String three = "3";
		assertTrue(table.put(3, three) == null);
		assertTrue(table.get(3) == three);
	}

	public void testSparseOpenTable() {

		Factory<Integer, String> factory = new Factory<Integer, String>() {
			@Override
			public String create(Integer key) throws Exception {
				if (key == 0)
					throw new UnsupportedOperationException("NYI");
				else
					return "" + key;
			}
		};

		Table<Integer, String> table = new SparseOpenTable<Integer, String>(new ConcurrentHashMap<Integer, String>(), factory);

		testTable(table);
	}

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

	public void testSparseOpenTableRaceCondition() throws Exception {

		final CountDownLatch latch = new CountDownLatch(1);

		SparseOpenTable.Factory<Integer, String> factory = new SparseOpenTable.Factory<Integer, String>() {
			@Override
			public String create(Integer key) throws Exception {
				latch.await();
				return "" + key;
			}
		};

		final Table<Integer, String> table = new SparseOpenTable<Integer, String>(new ConcurrentHashMap<Integer, String>(), factory);

		Getter getter1 = new Getter(0, table);
		Getter getter2 = new Getter(0, table);

		Thread thread1 = new Thread(getter1);
		Thread thread2 = new Thread(getter2);

		// line up two threads on latch in factory.create - one will lose the race
		// exercising that code path...
		thread1.start();
		thread2.start();
		// let them run...
		latch.countDown();
		
		thread1.join();
		thread2.join();
		
		String value1 = getter1.getValue();
		String value2 = getter2.getValue();
		assertTrue(value1.equals("0") && value2.equals("0") && value1 == value2);
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
