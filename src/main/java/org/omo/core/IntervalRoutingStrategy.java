/**
 * 
 */
package org.omo.core;

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
import org.omo.cash.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntervalRoutingStrategy implements Router.Strategy<Integer, Integer, Trade> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final NavigableMap<Date, Integer> dateToRoute;
	private final Collection<View<Integer, Trade>>[] routeToViews;
	
	public IntervalRoutingStrategy(Map<Interval, Collection<View<Integer, Trade>>> dateRangeToViews) {
		NavigableSet<DateTime> dates = new TreeSet<DateTime>();
		// aggregate period edges...
		for (Interval dateRange : dateRangeToViews.keySet()) {
			dates.add(dateRange.getStart());
			dates.add(dateRange.getEnd().minus(Period.millis(1)));
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
			Collection<View<Integer, Trade>> views = new ArrayList<View<Integer,Trade>>();
			routeToViews[route] = views;
			for (Entry<Interval, Collection<View<Integer, Trade>>> entry : dateRangeToViews.entrySet()) {
				boolean matched = false;
				Interval key = entry.getKey();
				Collection<View<Integer, Trade>> value = entry.getValue();
				if (key.contains(date)) {
					// map route to views
					views.addAll(value);
				}
			}
			route++;
		}
	}
	
	@Override
	public boolean getMutable() {
		return true;
	}

	@Override
	public Integer getRoute(Trade value) {
		//logger.warn("routing {} to {}", value.getValueDate(), dateToRoute.floorEntry(value.getValueDate()).getValue());
		return dateToRoute.floorEntry(value.getValueDate()).getValue();
	}

	@Override
	public Collection<View<Integer, Trade>> getViews(Integer route) {
		return routeToViews[route];
	}
	
}
