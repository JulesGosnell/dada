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
import java.util.Collection;

import junit.framework.TestCase;

import org.dada.core.BigDecimalSum.Factory;

public class BigDecimalAggregatorTestCase extends TestCase {

	public void test() throws Exception {
		String name = "Strategy";
		Integer inputKey = 1;
		Integer outputKey = 1;

		Creator<Amount> creator = new Creator<Amount>(){
			@Override
			public Amount create(Object... args) {
				return new Amount((Integer)args[0], (Integer)args[1], (BigDecimal)args[2]);
			}
		};
		
		Metadata<Integer, Amount> metadata = new IntrospectiveMetadata<Integer, Amount>(Amount.class, creator, "Id");

		Factory<Integer, Amount, Integer> factory = new Factory<Integer, Amount, Integer>() {
			@Override
			public Amount create(Integer outputKey, int version, Integer inputKey, BigDecimal amount) {
				return new Amount(outputKey, version, amount);
			}
		};
		
		Getter<BigDecimal, Amount> getter = new Getter<BigDecimal, Amount>() {
			@Override
			public BigDecimal get(Amount value) {
				return value.getAmount();
			}
		};

		BigDecimalSum<Integer, Amount, Integer, Amount> aggregator = new BigDecimalSum<Integer, Amount, Integer, Amount>(name, inputKey, outputKey, metadata, factory, getter);

		Collection<Amount> before = aggregator.getData();
		assertTrue(before.size() == 1);
		assertTrue(before.iterator().next().getAmount().equals(BigDecimal.ZERO));

		Collection<Update<Amount>> insertions = new ArrayList<Update<Amount>>();
		Collection<Update<Amount>> updates = new ArrayList<Update<Amount>>();
		Collection<Update<Amount>> deletions = new ArrayList<Update<Amount>>();

		// 1 + (2 - 1) - 1 = 1
		insertions.add(new Update<Amount>(null, new Amount(0, 0, BigDecimal.ONE)));
		updates.add(new Update<Amount>(new Amount(1, 0, BigDecimal.ONE), new Amount(0, 0, new BigDecimal("2"))));
		deletions.add(new Update<Amount>(new Amount(0, 0, BigDecimal.ONE), null));
		aggregator.update(insertions, updates, deletions);

		Collection<Amount> after = aggregator.getData();
		assertTrue(after.size() == 1);
		assertTrue(after.iterator().next().getAmount().equals(BigDecimal.ONE));
	}
}
