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
import java.util.HashMap;
import java.util.Map;

public class MetaModelImpl extends AbstractModel<String, String> implements MetaModel, View<Model<Object, Object>> {

	private final Map<String, Model<Object, Object>> nameToModel = new HashMap<String, Model<Object, Object>>();

	public MetaModelImpl(String name, Metadata<String, String> metadata) {
		super(name, metadata);
	}

	@Override
	public synchronized Data<String> getData() {
		return new Data<String>(new ArrayList<String>(nameToModel.keySet()), null);
	}
	
	@Override
	public synchronized Model<Object, Object> getModel(String modelName) {
		return nameToModel.get(modelName);
	}

	@Override
	public synchronized void update(Collection<Update<Model<Object, Object>>> insertions, Collection<Update<Model<Object, Object>>> alterations, Collection<Update<Model<Object, Object>>> deletions) {
		Collection<Update<String>> i;
		if (!insertions.isEmpty()) {
			i = new ArrayList<Update<String>>(insertions.size());
			for (Update<Model<Object, Object>> insertion : insertions) {
				Model<Object, Object> model = insertion.getNewValue();
				String modelName = model.getName();
				nameToModel.put(modelName, model);
				i.add(new Update<String>(null ,modelName));
			}
		} else {
			i = Collections.emptyList();
		}

		Collection<Update<String>> u;
		if (!alterations.isEmpty()) {
			u = new ArrayList<Update<String>>(alterations.size());
			for (Update<Model<Object, Object>> update : alterations) {
				Model<Object, Object> oldModel = update.getOldValue();
				Model<Object, Object> newModel = update.getNewValue();
				String oldModelName = oldModel.getName();
				String newModelName = newModel.getName();
				nameToModel.remove(oldModelName);
				nameToModel.put(newModelName, newModel);
				u.add(new Update<String>(oldModelName, newModelName));
			}
		} else {
			u = Collections.emptyList();
		}

		Collection<Update<String>> d;
		if (!deletions.isEmpty()) {
			d = new ArrayList<Update<String>>(deletions.size());
			for (Update<Model<Object, Object>> deletion : deletions) {
				Model<Object, Object> model = deletion.getOldValue();
				String modelName = model.getName();
				nameToModel.remove(modelName);
				d.add(new Update<String>(modelName, null));
			}
		} else {
			d = Collections.emptyList();
		}

		notifyUpdate(i, u, d);
		
	}

}
