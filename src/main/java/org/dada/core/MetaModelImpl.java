package org.dada.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.dada.jms.RemotingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaModelImpl extends AbstractModel<String, String> implements MetaModel, View<String, Model<Object, Object>> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final RemotingFactory<Model<Object, Object>> remotingFactory;
	private final ExecutorService executorService;
	private final Set<String> exportedModelNames = new HashSet<String>();
	private final Map<String, Model<Object, Object>> nameToModel = new ConcurrentHashMap<String, Model<Object,Object>>();
	
	public MetaModelImpl(String name, Metadata<String, String> metadata, RemotingFactory<Model<Object, Object>> remotingFactory, ExecutorService executorService) {
		super(name, metadata);
		this.remotingFactory = remotingFactory;
		this.executorService = executorService;
		exportedModelNames.add(name); // assume that we are exported - TODO - clean up using Transport interface
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
	}

	@Override
	public Registration<Object, Object> registerView(String modelName, View<Object, Object> view) {
		Model<Object, Object> model = nameToModel.get(modelName);
		try {
			if (!exportedModelNames.contains(modelName)) {
				logger.info("exporting Model: {}", model);
				remotingFactory.createServer(model, modelName, executorService);
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
	public void start() {
	}

	@Override
	public void stop() {
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
			Model<Object, Object> model = deletion.getNewValue();
			nameToModel.remove(model.getName());
		}
	}

}
