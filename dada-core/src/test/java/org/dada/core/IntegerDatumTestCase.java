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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

public class IntegerDatumTestCase extends TestCase {

	public void test() {
		int id = 1;
		int version = 2;
		IntegerDatum one = new IntegerDatum(id, version);
		
		assertTrue(one.getId() == id);
		assertTrue(one.getVersion() == version);
		
		IntegerDatum three = new IntegerDatum(3, 4);
		assertTrue(one.compareTo(three) < 0);
		
		IntegerDatum zero = new IntegerDatum(0, 0);
		
		Set<IntegerDatum> set = new TreeSet<IntegerDatum>();
		set.add(one);
		set.add(three);
		set.add(zero);
		Iterator<IntegerDatum> iterator = set.iterator();
		assertTrue(iterator.next() == zero);
		assertTrue(iterator.next() == one);
		assertTrue(iterator.next() == three);
	}
}
