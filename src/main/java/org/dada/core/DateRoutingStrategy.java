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

public class DateRoutingStrategy<V> implements Router.Strategy<Integer, V> {

	private final NavigableMap<Long, Integer> dateToRoute;
	private final Collection<View<Integer, V>>[] routeToViews;
	private final Getter<Long, V> getter;
	private final boolean mutable;

	// TODO: review decision to use Joda Time
	// TODO: consider cost of auto-[un]boxing...
	@SuppressWarnings("unchecked")
	public DateRoutingStrategy(Map<Interval, Collection<View<Integer, V>>> intervalToViews, Getter<Long, V> getter, boolean mutable) {
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
			Collection<View<Integer, V>> views = new ArrayList<View<Integer, V>>();
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
		
		this.getter = getter;
		this.mutable = mutable;
	}

	@Override
	public Collection<View<Integer, V>> getViews(int route) {
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
