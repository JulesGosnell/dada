/**
 * 
 */
package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.omo.cash.Trade;

public class DateRangeRoutingStrategy implements Router.Strategy<Integer, Integer, Trade> {

	private final TreeMap<Date, Integer> dateToRoute;
	private final Collection<View<Integer, Trade>>[] routeToViews;
	
	public DateRangeRoutingStrategy(Map<DateRange, Collection<View<Integer, Trade>>> dateRangeToViews) {
		TreeSet<Date> dates = new TreeSet<Date>();
		// aggregate period edges...
		for (DateRange dateRange : dateRangeToViews.keySet()) {
			dates.add(dateRange.getMin());
			dates.add(dateRange.getMax());
		}
		// add bounding edges
		dates.add(new Date(0));
		dates.add(new Date(dates.last().getTime() + 1));
		
		// map edges to routes...
		int route = 0;
		dateToRoute = new TreeMap<Date, Integer>();
		routeToViews = new Collection[dates.size()];
		for (Date date : dates) {
			dateToRoute.put(date, route);
			Collection<View<Integer, Trade>> views = new ArrayList<View<Integer,Trade>>();
			routeToViews[route] = views;
			for (Entry<DateRange, Collection<View<Integer, Trade>>> entry : dateRangeToViews.entrySet()) {
				boolean matched = false;
				DateRange key = entry.getKey();
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
		return dateToRoute.floorEntry(value.getValueDate()).getValue();
	}

	@Override
	public Collection<View<Integer, Trade>> getViews(Integer route) {
		return routeToViews[route];
	}
	
}