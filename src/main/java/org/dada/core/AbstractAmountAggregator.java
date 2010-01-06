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
import java.util.Collections;

public abstract class AbstractAmountAggregator<KI, VI extends Datum<KI>, KO, VO extends Datum<KO>> extends AbstractModel<KO, VO> implements View<KI, VI> {

	public interface Factory<KO, VO,KI> {
		VO create(KO outputKey, int version, KI inputKey, BigDecimal amount);
	}
	
	private final Collection<Update<VO>> nil = new ArrayList<Update<VO>>();
	private final KI inputKey;
	private final KO outputKey;
	private final Factory<KO, VO, KI> factory;
	
	private int version;
	private BigDecimal amount = BigDecimal.ZERO;
	
	public AbstractAmountAggregator(String name, KI inputKey, KO outputKey, Metadata<KO, VO> metadata, Factory<KO, VO, KI> factory) {
		super(name, metadata);
		this.inputKey = inputKey;
		this.outputKey = outputKey;
		this.factory = factory;
	}

	protected abstract BigDecimal getAmount(VI value);
	
	@Override
	public Collection<VO> getData() {
		int snapshotVersion;
		BigDecimal snapshotAmount;
		synchronized (this) {
			snapshotVersion = version;
			snapshotAmount = amount;
		}
		return Collections.singleton(factory.create(outputKey, snapshotVersion, inputKey, snapshotAmount));
	}

	@Override
	public void update(Collection<Update<VI>> insertions, Collection<Update<VI>> updates, Collection<Update<VI>> deletions) {
		BigDecimal delta = BigDecimal.ZERO;
		for (Update<VI> insertion : insertions) {
			VI newValue = insertion.getNewValue();
			BigDecimal insertionAmount = getAmount(newValue);
			delta = delta.add(insertionAmount);
		}
		for (Update<VI> update : updates) {
			delta = delta.subtract(getAmount(update.getOldValue()));
			delta = delta.add(getAmount(update.getNewValue()));
		}
		for (Update<VI> update : updates) {
			delta = delta.subtract(getAmount(update.getOldValue()));
		}
		int oldVersion, newVersion;
		BigDecimal oldAmount, newAmount;
		synchronized (this) {
			oldVersion = version;
			oldAmount = amount;
			newVersion = ++version;
			newAmount = (amount = amount.add(delta));
		}
		VO oldValue = factory.create(outputKey, oldVersion, inputKey, oldAmount);
		VO newValue = factory.create(outputKey, newVersion, inputKey, newAmount);
		Collection<Update<VO>> updatesOut = Collections.singleton(new Update<VO>(oldValue, newValue));
		notifyUpdate(nil, updatesOut, nil);
	}

}
