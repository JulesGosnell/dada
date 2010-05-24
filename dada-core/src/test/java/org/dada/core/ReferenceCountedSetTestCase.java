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

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;

public class ReferenceCountedSetTestCase extends TestCase {

	public void test() {
		Set<Integer> integers = new ReferenceCountedSet<Integer>();
		assertTrue(integers.isEmpty());
		assertTrue(integers.size() == 0);
		assertTrue(!integers.iterator().hasNext());
		assertTrue(integers.toArray().length == 0);
		assertTrue(integers.toArray(new Integer[0]).length == 0);
		
		Integer integer = 1;
		
		// removal of non-existent member
		assertTrue(!integers.remove(integer));
		
		// simple insertion
		assertTrue(integers.add(integer));
		assertTrue(integers.contains(integer));
		assertTrue(integers.containsAll(Collections.singleton(integer)));
		assertTrue(!integers.isEmpty());
		assertTrue(integers.size() == 1);
		
		// simple removal
		assertTrue(integers.remove(integer));
		assertTrue(!integers.contains(integer));
		assertTrue(integers.isEmpty());
		assertTrue(integers.size() == 0);
		
		// double insertion
		assertTrue(integers.add(integer));
		assertFalse(integers.addAll(Collections.singleton(integer)));
		assertTrue(integers.contains(integer));
		assertTrue(!integers.isEmpty());
		assertTrue(integers.size() == 1);

		// double removal
		assertFalse(integers.remove(integer));
		assertTrue(integers.contains(integer)); // set should still contain integer until second removal
		assertTrue(!integers.isEmpty());
		assertTrue(integers.size() == 1);
		assertTrue(integers.removeAll(Collections.singleton(integer)));
		assertTrue(!integers.contains(integer));
		assertTrue(integers.isEmpty());
		assertTrue(integers.size() == 0);
		
		integers.clear();
	}
}
