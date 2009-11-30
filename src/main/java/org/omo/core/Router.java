package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

public class Router<R, K, V> implements View<K, V> {
	
	// TODO: if we managed the MultiMaps via this API we could optimise them
	// to arrays when dealing with immutable attributes
	public interface Strategy<R, K, V> {
		boolean getMutable();
		R getRoute(V value);
		Collection<View<K, V>> getViews(R route);
	}
	
	private final Strategy<R, K, V> strategy;
	private final boolean mutable;
	
	public Router(Strategy<R, K, V> strategy) {
		this.strategy = strategy;
		this.mutable = strategy.getMutable();
	}

	private Collection<Update<V>> empty = new ArrayList<Update<V>>(0);
	
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		
//		if (insertions.size()==1 && updates.size()==0 && deletions.size()==0) {
//			for (Update<V> insertion : insertions) {
//				for (View<K, V> view : strategy.getViews(strategy.getRoute(insertion.getNewValue()))) {
//					view.update(insertions, updates, deletions);
//				}
//			}
//			return;
//		}
		
		// split updates according to Route...
		MultiMap routeToInsertions = new MultiValueMap();
		MultiMap routeToUpdates = new MultiValueMap();
		MultiMap routeToDeletions= new MultiValueMap();

		for (Update<V> insertion : insertions) {
			R route = strategy.getRoute(insertion.getNewValue());
			routeToInsertions.put(route, insertion);
		} 
		for (Update<V> update : updates) {
			R newRoute = strategy.getRoute(update.getNewValue());
			R oldRoute;
			if (mutable && !(oldRoute = strategy.getRoute(update.getOldValue())).equals(newRoute)) {
				routeToInsertions.put(newRoute, update);
				routeToDeletions.put(oldRoute, update);
			} else {
				routeToUpdates.put(newRoute, update);
			}
		}
		for (Update<V> deletion : deletions) {
			R route = strategy.getRoute(deletion.getOldValue());
			routeToInsertions.put(route, deletion);
		}
		// then dispatch on viewers...
		// TODO: optimise for single update case...
		Set<R> routes = new HashSet<R>();
		routes.addAll(routeToInsertions.keySet());
		routes.addAll(routeToUpdates.keySet());
		routes.addAll(routeToDeletions.keySet());
		for (R route : routes) {
			Collection<Update<V>> insertionsOut = (Collection<Update<V>>)routeToInsertions.get(route);
			Collection<Update<V>> updatesOut = (Collection<Update<V>>)routeToUpdates.get(route);
			Collection<Update<V>> deletionsOut = (Collection<Update<V>>)routeToDeletions.get(route);
			for (View<K, V> view : strategy.getViews(route)) {
				view.update(insertionsOut==null?empty:insertionsOut, updatesOut==null?empty:updatesOut, deletionsOut==null?empty:deletionsOut);
			}
		}
	}

}
