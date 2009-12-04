package org.omo.cash;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.omo.core.SparseOpenTable;
import org.omo.core.Table;
import org.omo.core.View;
import org.omo.core.Router.Strategy;

public class AccountRoutingStrategy implements Strategy<Integer, Trade> {

	private final Table<Integer, Collection<View<Integer, Trade>>> routeToViews;
	
	public AccountRoutingStrategy(SparseOpenTable.Factory<Integer, Collection<View<Integer,Trade>>> factory) {
		routeToViews = new SparseOpenTable<Integer, Collection<View<Integer,Trade>>>(new ConcurrentHashMap<Integer, Collection<View<Integer, Trade>>>(), factory);
	}
	
	@Override
	public boolean getMutable() {
		return true;
	}

	@Override
	public int getRoute(Trade value) {
		return value.getAccount();
	}

	@Override
	public Collection<View<Integer, Trade>> getViews(int route) {
		return routeToViews.get(route);
	}
}
