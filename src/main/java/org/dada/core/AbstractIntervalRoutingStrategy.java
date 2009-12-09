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
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

// TODO: move completely to Joda time...
public abstract class AbstractIntervalRoutingStrategy<V> implements Router.Strategy<Integer, V> {

	private final NavigableMap<Date, Integer> dateToRoute;
	private final Collection<View<Integer, V>>[] routeToViews;

	public AbstractIntervalRoutingStrategy(Map<Interval, Collection<View<Integer, V>>> intervalToViews) {
		NavigableSet<DateTime> dates = new TreeSet<DateTime>();
		// aggregate period edges...
		for (Interval interval : intervalToViews.keySet()) {
			dates.add(interval.getStart());
			dates.add(interval.getEnd().minus(Period.millis(1)));
		}
		// add bounding edges
		dates.add(new DateTime(0));
		dates.add(new DateTime(dates.last().plus(Period.millis(1))));

		// map edges to routes...
		int route = 0;
		dateToRoute = new TreeMap<Date, Integer>();
		routeToViews = new Collection[dates.size()];
		for (DateTime date : dates) {
			dateToRoute.put(date.toDate(), route);
			Collection<View<Integer, V>> views = new ArrayList<View<Integer,V>>();
			routeToViews[route] = views;
			for (Entry<Interval, Collection<View<Integer, V>>> entry : intervalToViews.entrySet()) {
				Interval key = entry.getKey();
				Collection<View<Integer, V>> value = entry.getValue();
				if (key.contains(date)) {
					// map route to views
					views.addAll(value);
				}
			}
			route++;
		}
	}

	public int getRoute(Date date) {
		return dateToRoute.floorEntry(date).getValue();
	}

	@Override
	public Collection<View<Integer, V>> getViews(int route) {
		return routeToViews[route];
	}

}
