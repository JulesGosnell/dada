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

import javax.jms.Destination;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class BatcherTestCase extends MockObjectTestCase {
	
//	public void testDelayInducedFlush() {
//
//		final View<Integer, Datum<Integer>> view = (View)mock(View.class);
//		Collection<View<Integer, Datum<Integer>>> views = new ArrayList<View<Integer,Datum<Integer>>>();
//		views.add(view);
//
//		// test delay-induced flush
//		int maxSize = 1000000;
//		long maxDelay = 0;
//		Batcher<Integer, Datum<Integer>> batcher = new Batcher<Integer, Datum<Integer>>(maxSize, maxDelay, views);
//		
//		Datum<Integer> datum = new IntegerDatum(0, 0);
//		final Collection<Update<Datum<Integer>>> insertions = Collections.singleton(new Update<Datum<Integer>>(null, datum));
//		final Collection<Update<Datum<Integer>>> nil = new ArrayList<Update<Datum<Integer>>>();
//
//		checking(new Expectations(){{
//            one(view).update(insertions, nil, nil);
//        }});
//		
//		batcher.update(insertions, nil, nil);
//	}
	
	public void testSizeInducedFlush() {

		final View<Integer, Datum<Integer>> view = (View)mock(View.class);
		Collection<View<Integer, Datum<Integer>>> views = new ArrayList<View<Integer,Datum<Integer>>>();
		views.add(view);

		int maxSize = 1;
		long maxDelay = 1000000;
		Batcher<Integer, Datum<Integer>> batcher = new Batcher<Integer, Datum<Integer>>(maxSize, maxDelay, views);
		
		final Collection<Update<Datum<Integer>>> nil = new ArrayList<Update<Datum<Integer>>>();

		Datum<Integer> datum0 = new IntegerDatum(0, 0);
		Datum<Integer> datum1 = new IntegerDatum(1, 0);

		// pass straight through...

		final Collection<Update<Datum<Integer>>> insertions = new ArrayList<Update<Datum<Integer>>>();
		insertions.add(new Update<Datum<Integer>>(null, datum0));
		insertions.add(new Update<Datum<Integer>>(null, datum1));

		checking(new Expectations(){{
			one(view).update(insertions, nil, nil);
		}});

		batcher.update(insertions, nil, nil);	

		// overflow...

		Collection<Update<Datum<Integer>>> insertions1 = Collections.singleton(new Update<Datum<Integer>>(null, datum0));
		batcher.update(insertions1, nil, nil);

		checking(new Expectations(){{
			one(view).update(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
		}});

		Collection<Update<Datum<Integer>>> insertions2 = Collections.singleton(new Update<Datum<Integer>>(null, datum1));
		batcher.update(insertions2, nil, nil);

	}

}
