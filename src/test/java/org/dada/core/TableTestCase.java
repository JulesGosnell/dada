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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public class TableTestCase extends TestCase {

	public void doNottestCompactOpenTable() {

		CompactOpenTable.Factory<String> factory = new CompactOpenTable.Factory<String>() {

			@Override
			public String create(Integer key, Collection<String> views) {
				return "" + key;
			}
		};

		Table<Integer, String> table = new CompactOpenTable<String>(new ArrayList<String>(), factory);

		testTable(table);
	}

	public void testSparseOpenTable() {

		SparseOpenTable.Factory<Integer, String> factory = new SparseOpenTable.Factory<Integer, String>() {

			// TODO: map should not be passed as a param here - but rather to ctor, if needed...
			@Override
			public String create(Integer key, ConcurrentMap<Integer, String> map) throws Exception {
				if (key < 0)
					throw new UnsupportedOperationException("NYI");
				else
					return "" + key;
			}
		};

		Table<Integer, String> table = new SparseOpenTable<Integer, String>(new ConcurrentHashMap<Integer, String>(), factory);

		testTable(table);
	}

	protected boolean lose;

//	public void testSparseOpenTableRaceCondition() {
//
//		final CountDownLatch latch = new CountDownLatch(1);
//
//		SparseOpenTable.Factory<Integer, String> factory = new SparseOpenTable.Factory<Integer, String>() {
//			@Override
//			public String create(Integer key, ConcurrentMap<Integer, String> map) throws Exception {
//				if (lose)
//					latch.await();
//				return "" + key;
//			}
//		};
//
//		final Table<Integer, String> table = new SparseOpenTable<Integer, String>(new ConcurrentHashMap<Integer, String>(), factory);
//
//		Runnable putter = new Runnable() {
//			@Override
//			public void run() {
//				table.get(0);
//			}
//		};
//
//		Thread loser = new Thread(putter);
//		Thread winner = new Thread(putter);
//
//		lose = true;
//		loser.start();
//		// aarrgh !!
//		winner.start();
//		winner.join();
//		latch.countDown();
//		loser.join();
//
//	}

	public void testTable(Table<Integer, String> table) {

		assertTrue(table.get(0).equals("0"));

		String one = "1";
		table.put(1, one);
		assertTrue(table.get(1) == one);

		assertTrue(table.get(-1) == null);

		String minusOne = "-1";
		table.put(-1, minusOne);
		assertTrue(table.get(-1) == minusOne);
	}
}
