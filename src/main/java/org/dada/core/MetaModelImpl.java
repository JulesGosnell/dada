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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dada.slf4j.Logger;
import org.dada.slf4j.LoggerFactory;

public class MetaModelImpl extends AbstractModel<String, String> implements MetaModel, View<Model<Object, Object>> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ServiceFactory<Model<Object, Object>> serviceFactory;
	private final Set<String> exportedModelNames = new HashSet<String>();
	private final Map<String, Model<Object, Object>> nameToModel = new ConcurrentHashMap<String, Model<Object, Object>>();
	private volatile QueryEngine engine;
	private final Map<String, Model<?, ?>> queryToModel = new HashMap<String, Model<?,?>>();
	

	public MetaModelImpl(String name, Metadata<String, String> metadata, ServiceFactory<Model<Object, Object>> serviceFactory) {
		super(name, metadata);
		this.serviceFactory = serviceFactory;
		exportedModelNames.add(name); // assume that we are exported - TODO - clean up using ServiceFactory interface
	}

	@Override
	public Collection<String> getData() {
		return nameToModel.keySet();
	}

	@Override
	public Collection<Object> deregisterView(String modelName, View<Object> view) {
		Model<Object, Object> model = nameToModel.get(modelName);
		logger.info("deregistering View ({}) from Model ({})", view, model);
		return model.deregisterView(view);
		// TODO - what about tidying up ServiceFactory resources ? Their allocation should be done
		// on a first-in-turns-on-lights, last-out-turns-off-lights basis...
	}

	@Override
	public Registration<Object, Object> registerView(String modelName, View<Object> view) {
		Model<Object, Object> model = nameToModel.get(modelName);
		try {
			if (!exportedModelNames.contains(modelName)) {
				logger.info("exporting Model: {}", model);
				serviceFactory.server(model, modelName);
				exportedModelNames.add(modelName);
			}
			logger.info("registering View ({}) with Model ({})", view, model);
			return model.registerView(view);
		} catch (Exception e) {
			logger.error("unable to export Model: {}", e, model);
			return null;
		}
	}

	@Override
	public void update(Collection<Update<Model<Object, Object>>> insertions, Collection<Update<Model<Object, Object>>> updates, Collection<Update<Model<Object, Object>>> deletions) {
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
		if (!updates.isEmpty()) {
			u = new ArrayList<Update<String>>(updates.size());
			for (Update<Model<Object, Object>> update : updates) {
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
	
	public void setQueryEngine(QueryEngine engine) {
		this.engine = engine;
	}

	@Override
	public Collection<Object> deregisterQueryView(String query, View<Object> view) {
		synchronized (queryToModel) {
			Model<?, ?> model = queryToModel.get(query);
			if (model == null)
				return null;
			else
				return deregisterView(model.getName(), view);
		}
	}

	@Override
	public Registration<Object, Object> registerQueryView(String query, View<Object> view) {
		synchronized (queryToModel) {
			Model<?, ?> model = queryToModel.get(query);
			if (model == null) {
				if (engine == null)
					throw new IllegalStateException("no query engine available");
				queryToModel.put(query, model = engine.query(query));
			}
		}
		return registerView(engine.query(query).getName(), view);
	}

}
