package org.omo.cash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.omo.core.CompactTable;
import org.omo.core.Table;
import org.omo.core.View;
import org.omo.core.Router.Strategy;

public class CurrencyRoutingStrategy implements Strategy<Integer, Trade> {

	private final Table<Collection<View<Integer, Trade>>> routeToViews;
	
	public CurrencyRoutingStrategy(Collection<View<Integer, Trade>> views) {
		List<Collection<View<Integer, Trade>>> tmp = new ArrayList<Collection<View<Integer, Trade>>>();
		for (View<Integer, Trade> view : views) {
			tmp.add(Collections.singleton(view));
		}
		routeToViews = new CompactTable<Collection<View<Integer,Trade>>>(tmp, null);
	}
	
	@Override
	public boolean getMutable() {
		return true;
	}

	@Override
	public int getRoute(Trade value) {
		return value.getCurrency();
	}

	@Override
	public Collection<View<Integer, Trade>> getViews(int route) {
		return routeToViews.get(route);
	}
}
