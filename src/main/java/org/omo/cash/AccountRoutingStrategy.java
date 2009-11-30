package org.omo.cash;

import java.util.Collection;

import org.omo.core.View;
import org.omo.core.Router.Strategy;

import edu.emory.mathcs.backport.java.util.Collections;

public class AccountRoutingStrategy implements Strategy<Integer, Trade> {

	private final Collection<View<Integer, Trade>>[] views;
	
	public AccountRoutingStrategy(Collection<View<Integer, Trade>> views) {
		int i = 0;
		this.views = new Collection[views.size()];
		for (View<Integer, Trade> view : views) {
			this.views[i++] = Collections.singleton(view);
		}
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
		return views[route];
	}
}
