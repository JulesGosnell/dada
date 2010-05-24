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
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.joda.time.Interval;

public class DateRoutingStrategy<V> implements Router.Strategy<V> {

	private final NavigableMap<Long, Integer> dateToRoute;
	private final Collection<View<V>>[] routeToViews;
	private final Getter<Long, V> getter;
	private final boolean mutable;

	// TODO: review decision to use Joda Time
	// TODO: consider cost of auto-[un]boxing...
	@SuppressWarnings("unchecked")
	public DateRoutingStrategy(Map<Interval, Collection<View<V>>> intervalToViews, Getter<Long, V> getter, boolean mutable) {
		NavigableSet<Long> dates = new TreeSet<Long>();
		// aggregate period edges...
		for (Interval interval : intervalToViews.keySet()) {
			dates.add(interval.getStart().getMillis());
			dates.add(interval.getEnd().getMillis() - 1L);
		}
		// add bounding edges
		dates.add(0L);
		dates.add(dates.last() + 1L);

		// map edges to routes...
		int route = 0;
		dateToRoute = new TreeMap<Long, Integer>();
		routeToViews = new Collection[dates.size()]; // unchecked :-(
		for (Long date : dates) {
			dateToRoute.put(date, route);
			Collection<View<V>> views = new ArrayList<View<V>>();
			routeToViews[route] = views;
			for (Entry<Interval, Collection<View<V>>> entry : intervalToViews.entrySet()) {
				Interval key = entry.getKey();
				Collection<View<V>> value = entry.getValue();
				if (key.contains(date)) {
					// map route to views
					views.addAll(value);
				}
			}
			route++;
		}

		this.getter = getter;
		this.mutable = mutable;
	}

	@Override
	public Collection<View<V>> getViews(int route) {
		return routeToViews[route];
	}

	@Override
	public boolean getMutable() {
		return mutable;
	}

	@Override
	public int getRoute(V value) {
		return dateToRoute.floorEntry(getter.get(value)).getValue();
	}

}
