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
import java.util.Collection;

public class BigDecimalAggregator<KI, VI, KO, VO> extends AggregatedModelView<KI, VI, BigDecimal, KO, VO> {

	public interface Factory<KO, VO, KI> {
		VO create(KO outputKey, int version, KI inputKey, BigDecimal amount);
	}

	public BigDecimalAggregator(String name, KI inputKey, KO outputKey, Metadata<KO, VO> metadata, final Factory<KO, VO, KI> factory, final Getter<BigDecimal, VI> getter) {
		super(name, metadata, outputKey, new Aggregator<VI, BigDecimal, KO, VO>() {

			@Override
			public BigDecimal initialValue() {
				return BigDecimal.ZERO;
			}

			@Override
			public VO currentValue(KO key, int version, BigDecimal value) {
				return factory.create(key, version, null, value);
			}

			@Override
			public BigDecimal aggregate(Collection<Update<VI>> insertions, Collection<Update<VI>> updates, Collection<Update<VI>> deletions) {
				BigDecimal delta = BigDecimal.ZERO;
				
				for (Update<VI> insertion : insertions) {
					VI newValue = insertion.getNewValue();
					BigDecimal insertionAmount = getter.get(newValue);
					delta = delta.add(insertionAmount);
				}
				for (Update<VI> update : updates) {
					delta = delta.subtract(getter.get(update.getOldValue()));
					delta = delta.add(getter.get(update.getNewValue()));
				}
				for (Update<VI> update : updates) {
					delta = delta.subtract(getter.get(update.getOldValue()));
				}

				return delta;
			}

			@Override
			public BigDecimal apply(BigDecimal currentValue, BigDecimal delta) {
				return currentValue.add(delta);
			}
		});
	}

}
