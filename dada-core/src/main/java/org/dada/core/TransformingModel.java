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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransformingModel<IK, IV, OK, OV> extends AbstractModel<OK, OV> implements View<IV> {

	public interface Transformer<IV, OK, OV> {
		OV transform(IV value);
		OK getKey(OV value);
	}
	
	//private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Map<OK, OV> keyToData = new ConcurrentHashMap<OK, OV>();
	private final Transformer<IV, OK, OV> transformer;
	
	public TransformingModel(String name, Metadata<OK, OV> metadata, Transformer<IV, OK, OV> transformer) {
		super(name, metadata);
		this.transformer = transformer;
	}

	@Override
	public Data<OV> getData() {
		return new Data<OV>(keyToData.values(), null);
	}

	@Override
	public void update(Collection<Update<IV>> insertions, Collection<Update<IV>> alterations, Collection<Update<IV>> deletions) {
		Collection<Update<OV>> i;
		if (!insertions.isEmpty()) {
			i = new ArrayList<Update<OV>>(insertions.size());
			for (Update<IV> insertion : insertions) {
				IV inputValue = insertion.getNewValue();
				OV outputValue = transformer.transform(inputValue);
				OK outputKey = transformer.getKey(outputValue);
				keyToData.put(outputKey, outputValue);
				i.add(new Update<OV>(null ,outputValue));
			}
		} else {
			i = Collections.emptyList();
		}

		Collection<Update<OV>> u;
		if (!alterations.isEmpty()) {
			u = new ArrayList<Update<OV>>(alterations.size());
			for (Update<IV> update : alterations) {
				IV oldInputValue = update.getOldValue();
				IV newInputValue = update.getNewValue();
				OV oldOutputValue = transformer.transform(oldInputValue);
				OV newOutputValue = transformer.transform(newInputValue);
				keyToData.remove(transformer.getKey(oldOutputValue));
				keyToData.put(transformer.getKey(newOutputValue), newOutputValue);
				u.add(new Update<OV>(oldOutputValue, newOutputValue));
			}
		} else {
			u = Collections.emptyList();
		}

		Collection<Update<OV>> d;
		if (!deletions.isEmpty()) {
			d = new ArrayList<Update<OV>>(deletions.size());
			for (Update<IV> deletion : deletions) {
				IV inputValue = deletion.getOldValue();
				OV outputValue = transformer.transform(inputValue);
				keyToData.remove(transformer.getKey(outputValue));
				d.add(new Update<OV>(outputValue, null));
			}
		} else {
			d = Collections.emptyList();
		}

		notifyUpdate(i, u, d);
	}

	@Override
	public OV find(OK key) {
		// TODO Auto-generated method stub
		// return null;
		throw new UnsupportedOperationException("NYI");
	}

}
