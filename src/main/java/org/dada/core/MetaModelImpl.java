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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaModelImpl extends AbstractModel<String, String> implements MetaModel, View<String, Model<Object, Object>> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ServiceFactory<Model<Object, Object>> serviceFactory;
	private final Set<String> exportedModelNames = new HashSet<String>();
	private final Map<String, Model<Object, Object>> nameToModel = new ConcurrentHashMap<String, Model<Object,Object>>();
	
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
	public boolean deregisterView(String modelName, View<Object, Object> view) {
		Model<Object, Object> model = nameToModel.get(modelName);
		logger.info("deregistering View ({}) from Model ({})", view, model);
		return model.deregisterView(view);
		// TODO - what about tidying up ServiceFactory resources ? Their allocation should be done
		// on a first-in-turns-on-lights, last-out-turns-off-lights basis...
	}

	@Override
	public Registration<Object, Object> registerView(String modelName, View<Object, Object> view) {
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
			logger.error("unable to export Model: {}", model, e);
			return null;
		}
	}

	@Override
	public void update(Collection<Update<Model<Object, Object>>> insertions, Collection<Update<Model<Object, Object>>> updates, Collection<Update<Model<Object, Object>>> deletions) {
		for (Update<Model<Object, Object>> insertion : insertions) {
			Model<Object, Object> model = insertion.getNewValue();
			nameToModel.put(model.getName(), model);
		}
		for (Update<Model<Object, Object>> update : updates) {
			Model<Object, Object> model = update.getNewValue();
			nameToModel.put(model.getName(), model);
		}
		for (Update<Model<Object, Object>> deletion : deletions) {
			Model<Object, Object> model = deletion.getOldValue();
			nameToModel.remove(model.getName());
		}
	}

}
