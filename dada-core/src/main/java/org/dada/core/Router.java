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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

public class Router<V> implements View<V> {

	// TODO: if we managed the MultiMaps via this API we could optimise them
	// to arrays when dealing with immutable attributes
	public interface Strategy<V> {
		boolean getMutable();
		int getRoute(V value);
		Collection<View<V>> getViews(int route);
	}

	private final Strategy<V> strategy;
	private final boolean mutable;

	public Router(Strategy<V> strategy) {
		this.strategy = strategy;
		this.mutable = strategy.getMutable();
	}

	private Collection<Update<V>> empty = new ArrayList<Update<V>>(0);

	@SuppressWarnings("unchecked")
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> alterations, Collection<Update<V>> deletions) {

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
		MultiMap routeToDeletions = new MultiValueMap();

		for (Update<V> insertion : insertions) {
			int route = strategy.getRoute(insertion.getNewValue());
			routeToInsertions.put(route, insertion);
		}
		for (Update<V> update : alterations) {
			int newRoute = strategy.getRoute(update.getNewValue());
			int oldRoute;
			// the boolean test of 'mutable 'does not add any further constraint - it simply heads off a more expensive
			// test if possible - therefore we cannot produce coverage for the case where an immutable attribute is mutated...
			if (mutable && (oldRoute = strategy.getRoute(update.getOldValue())) != newRoute) {
				routeToInsertions.put(newRoute, update);
				routeToDeletions.put(oldRoute, update);
			} else {
				routeToUpdates.put(newRoute, update);
			}
		}
		for (Update<V> deletion : deletions) {
			int route = strategy.getRoute(deletion.getOldValue());
			routeToInsertions.put(route, deletion);
		}
		// then dispatch on viewers...
		// TODO: optimise for single update case...
		Set<Integer> routes = new HashSet<Integer>();
		routes.addAll(routeToInsertions.keySet());
		routes.addAll(routeToUpdates.keySet());
		routes.addAll(routeToDeletions.keySet());
		for (int route : routes) {
			Collection<Update<V>> insertionsOut = (Collection<Update<V>>) routeToInsertions.get(route);
			Collection<Update<V>> updatesOut = (Collection<Update<V>>) routeToUpdates.get(route);
			Collection<Update<V>> deletionsOut = (Collection<Update<V>>) routeToDeletions.get(route);
			for (View<V> view : strategy.getViews(route)) {
				view.update(insertionsOut == null ? empty : insertionsOut,
							updatesOut == null ? empty : updatesOut,
							deletionsOut == null ? empty : deletionsOut);
			}
		}
	}

}
