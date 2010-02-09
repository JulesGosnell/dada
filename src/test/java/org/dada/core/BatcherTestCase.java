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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class BatcherTestCase extends MockObjectTestCase {

	private final Collection<Update<Datum<Integer>>> nil = new ArrayList<Update<Datum<Integer>>>();
	

	public void testSizeInducedFlush() {

		final View<Datum<Integer>> view = (View)mock(View.class);
		Collection<View<Datum<Integer>>> views = new ArrayList<View<Datum<Integer>>>();
		views.add(view);

		int maxSize = 1;
		long maxDelay = 1000000;
		Batcher<Integer, Datum<Integer>> batcher = new Batcher<Integer, Datum<Integer>>(maxSize, maxDelay, views);
		
		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		Update<Datum<Integer>> update0 = new Update<Datum<Integer>>(null, datum0);
		Datum<Integer> datum1 = new IntegerDatum(1, 0);
		Update<Datum<Integer>> update1 = new Update<Datum<Integer>>(null, datum1);
		Datum<Integer> datum2 = new IntegerDatum(2, 0);
		Update<Datum<Integer>> update2 = new Update<Datum<Integer>>(null, datum2);

		// empty update...
		
		batcher.update(nil, nil, nil);	

		// pass straight through...

		final Collection<Update<Datum<Integer>>> insertions = new ArrayList<Update<Datum<Integer>>>();
		insertions.add(update0);
		insertions.add(update1);

		checking(new Expectations(){{
			one(view).update(insertions, nil, nil);
		}});

		batcher.update(insertions, nil, nil);	

		// pass straight through, picking up outstanding batch content...
		
		final Collection<Update<Datum<Integer>>> insertions1 = new ArrayList<Update<Datum<Integer>>>();
		insertions1.add(update0);
		batcher.update(insertions1, nil, nil);	

		final Collection<Update<Datum<Integer>>> insertions2 = new ArrayList<Update<Datum<Integer>>>();
		insertions2.add(update1);
		insertions2.add(update2);

		checking(new Expectations(){{
			one(view).update(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class))); // I want to check the contents...
		}});

		batcher.update(insertions2, nil, nil);

		// accumulate and overflow...

		Collection<Update<Datum<Integer>>> insertions3 = Collections.singleton(update0);
		batcher.update(insertions3, nil, nil);

		checking(new Expectations(){{
			one(view).update(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
		}});

		batcher.update(insertions3, nil, nil);
	}

	public void testTimerInducedFlush() throws Exception {

		final Datum<Integer> datum0 = new IntegerDatum(0, 0);
		final Update<Datum<Integer>> update0 = new Update<Datum<Integer>>(null, datum0);
		final Collection<Update<Datum<Integer>>> insertions0 = Collections.singleton(update0);

		final Datum<Integer> datum1 = new IntegerDatum(1, 0);
		final Update<Datum<Integer>> update1 = new Update<Datum<Integer>>(null, datum1);
		final Collection<Update<Datum<Integer>>> insertions1 = Collections.singleton(update1);

		final CountDownLatch latch = new CountDownLatch(1);
		final Thread thread = Thread.currentThread();
		final View<Datum<Integer>> view = new View<Datum<Integer>>() {
			@Override
			public void update(Collection<Update<Datum<Integer>>> insertions, Collection<Update<Datum<Integer>>> updates, Collection<Update<Datum<Integer>>> deletions) {
				assertTrue(insertions.size() == 2);
				assertFalse(Thread.currentThread().equals(thread)); // triggered by timer...
				latch.countDown();
			}
		};
		
		final Collection<View<Datum<Integer>>> views = new ArrayList<View<Datum<Integer>>>();
		views.add(view);

		final int maxSize = 1000000;
		final long maxDelay = 1000;
		final Batcher<Integer, Datum<Integer>> batcher = new Batcher<Integer, Datum<Integer>>(maxSize, maxDelay, views);

		batcher.update(insertions0, nil, nil);
		batcher.update(insertions1, nil, nil);
		
		assertTrue(latch.await(100, TimeUnit.SECONDS));
	}
}
