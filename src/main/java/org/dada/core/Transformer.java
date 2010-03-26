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

import org.dada.slf4j.Logger;
import org.dada.slf4j.LoggerFactory;

/**
 * A Connector that performs a one-for-one pluggable transformation on Updates flowing through it.

 * @author jules
 *
 * @param <IV>
 * @param <OV>
 */
public class Transformer<IV, OV> extends Connector<IV, OV> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Transformer.class);

	public interface StatelessStrategy<IV, OV> {
		OV transform(IV value);
	}
	
	public interface StatefulStrategy<IV, OV> {
		Update<OV> insert(Update<IV> insertion);
		Update<OV> update(Update<IV> alteration);
		Update<OV> delete(Update<IV> deletion);
	}
	
	private final StatefulStrategy<IV, OV> strategy;

	public Transformer(Collection<View<OV>> views, StatefulStrategy<IV, OV> strategy) {
		super(views);
		this.strategy = strategy;
	}

	public Transformer(Collection<View<OV>> views, final StatelessStrategy<IV, OV> strategy) {
		super(views);
		this.strategy = new StatefulStrategy<IV, OV>() {

			@Override
			public Update<OV> insert(Update<IV> insertion) {
				return new Update<OV>(null, strategy.transform(insertion.getNewValue()));
			}

			@Override
			public Update<OV> update(Update<IV> alteration) {
				return new Update<OV>(
						strategy.transform(alteration.getOldValue()),
						strategy.transform(alteration.getNewValue()));
			}

			@Override
			public Update<OV> delete(Update<IV> deletion) {
				return new Update<OV>(strategy.transform(deletion.getOldValue()), null);
			}
			
		};
	}

	@Override
	public void update(Collection<Update<IV>> insertions, Collection<Update<IV>> alterations, Collection<Update<IV>> deletions) {
		Collection<Update<OV>> i = new ArrayList<Update<OV>>(insertions.size());
		for (Update<IV> insertion :  insertions)
			i.add(strategy.insert(insertion));
		
		Collection<Update<OV>> a = new ArrayList<Update<OV>>(alterations.size());
		for (Update<IV> alteration :  alterations)
			a.add(strategy.update(alteration));

		Collection<Update<OV>> d = new ArrayList<Update<OV>>(deletions.size());
		for (Update<IV> deletion :  deletions)
			a.add(strategy.update(deletion));

		for (View<OV> view : getViews()) {
			try {
				view.update(i, a, d);
			} catch (Throwable t) {
				LOGGER.error("problem updating View", t);
			}
		}
	}

}
