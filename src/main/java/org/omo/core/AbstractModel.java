package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

// TODO - merge with more recent code - concurrent modification issues in listener collection...
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractModel<K, V> implements Model<K, V> {

	protected final Log log = LogFactory.getLog(getClass());

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
		log.debug("registering view: " + view);
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
			log.debug("deregistered view: " + view);
		else
			log.warn("failed to deregister view: " + view);
		
		return success;
	}	

	// TODO: notifications for update/delete

	protected void notifyUpdates(Collection<V> updates) {
		for (View<K, V> view : views)
			try {
				view.update(updates);
			} catch (RuntimeException e) {
				log.error("view notification failed: " + view + " <- " + updates, e);
			}
	}
}
