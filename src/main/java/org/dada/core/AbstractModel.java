package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModel<K, V> implements Model<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final String name;
	
	protected final Collection<View<K, V>> views = new ArrayList<View<K, V>>();

	protected final Metadata<K, V> metadata;
	
	protected abstract Collection<V> getData();
	
	public AbstractModel(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Registration<K, V> registerView(View<K, V> view) {
		logger.debug("registering view: {}", view);
		synchronized (views) {
			views.add(view);
		}
		return new Registration<K, V>(getMetadata(), getData());
	}
	
	private Metadata<K, V> getMetadata() {
		return metadata;
	}

	@Override
	public boolean deregisterView(View<K, V> view) {
		boolean success;
		synchronized (views) {
			success = views.remove(view);
		}
		if (success)
			logger.debug("deregistered view: {}", view);
		else
			logger.warn("failed to deregister view: {}", view);

		return success;
	}	

	protected void notifyUpdates(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		for (View<K, V> view : views)
			try {
				view.update(insertions, updates, deletions);
			} catch (RuntimeException e) {
				logger.error("view notification failed: {} <- {}", view, updates);
				logger.error("", e);
			}
	}
}
