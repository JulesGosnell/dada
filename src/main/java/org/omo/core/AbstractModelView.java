
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModelView<K, V> implements ModelView<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final String name;
	protected final Metadata<K, V> metadata;
	private final Object viewsLock = new Object();
	protected volatile List<View<K, V>> views = new ArrayList<View<K,V>>();
	protected volatile Collection<Aggregator<? extends Object, V>> aggregators = new ArrayList<Aggregator<? extends Object,V>>();
	
	public AbstractModelView(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getName() {
		return name;
	}

	public abstract Collection<V> getValues();

	@Override
	public Registration<K, V> registerView(View<K, V> view) {
		synchronized (viewsLock) {
			//views = (IPersistentSet)views.cons(view);
			List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
			newViews.add(view);
			views = newViews;
			logger.debug("{}: registered view: {}", name, view);
		}
		Collection<V> values = getValues();
		return new Registration<K, V>(metadata, new ArrayList<V>(values)); // TODO: hack - clojure containers not serialisable
	}
	
	@Override
	public boolean deregisterView(View<K, V> view) {
		try {
			synchronized (viewsLock) {
				//views = views.disjoin(view);
				List<View<K, V>> newViews = new ArrayList<View<K,V>>(views);
				newViews.remove(view);
				views = newViews;
				logger.debug("" + this + " deregistered view:" + view + " -> " + views);
			}
		} catch (Exception e) {
		    logger.error("unable to deregister view: {}", view);
		}
		return true;
	}

	protected void notifyUpdate(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		List<View<K, V>> snapshot = views;
		for (View<K, V> view : snapshot) {
			view.update(insertions, updates, deletions);
		}
	}

	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + name + ">";
	}

	// TODO: move aggregation up into an interface
	public Registration<K, V> register(Aggregator<? extends Object, V> aggregator) {
		Collection<Aggregator<? extends Object, V>> newAggregators = new ArrayList<Aggregator<? extends Object,V>>(aggregators);
		newAggregators.add(aggregator);
		aggregators = newAggregators;
		return new Registration<K, V>(null, getValues());
	}

	public void deregister(Aggregator<? extends Object, V> aggregator) {
		Collection<Aggregator<? extends Object, V>> newAggregators = new ArrayList<Aggregator<? extends Object,V>>(aggregators);
		newAggregators.remove(aggregator);
		aggregators = newAggregators;
	}

}
