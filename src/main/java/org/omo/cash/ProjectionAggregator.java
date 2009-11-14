package org.omo.cash;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.omo.core.Aggregator;
import org.omo.core.DateRange;
import org.omo.core.Update;
import org.omo.core.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionAggregator implements Aggregator<Projection, AccountTotal> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<BigDecimal> positions;
	private final List<Date> dates;
	private final int account;
	private final View<Integer, Projection> view;

	private int version;
	
	public ProjectionAggregator(String name, DateRange dateRange, int account, View<Integer, Projection> view) {
		this.account = account;
		this.view = view;
		dates = new ArrayList<Date>(dateRange.getValues()); // TODO: clumsy - but enables index lookup - slow - should be a Map ?
		positions = new ArrayList<BigDecimal>(dates.size());
		for (Date date : dates) {
			positions.add(new BigDecimal(0));
		}
	}
	
	@Override
	public Projection getAggregate() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	private Collection<Update<Projection>> empty = new ArrayList<Update<Projection>>(); 
	
	@Override
	public synchronized void insert(Collection<Update<AccountTotal>> insertionsIn) {
		logger.debug("insert: size={}", insertionsIn.size());
		Projection oldValue = new Projection(account, version, new ArrayList<BigDecimal>(positions));
		for (Update<AccountTotal> insertionIn : insertionsIn) {
			AccountTotal newValue = insertionIn.getNewValue();
			Date date = newValue.getId();
			int index = dates.indexOf(date);
			positions.set(index, newValue.getAmount());
		}
		Projection newValue = new Projection(account, version++, new ArrayList<BigDecimal>(positions));
		Set<Update<Projection>> updates = Collections.singleton(new Update<Projection>(oldValue, newValue));
		view.update(empty, updates, empty);
	}

	@Override
	public void remove(Collection<Update<AccountTotal>> value) {
		logger.debug("remove: size={}", 1);
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void update(AccountTotal oldValue, AccountTotal newValue) {
		logger.debug("update: {} -> {}", oldValue, newValue);
		Date date = newValue.getId();
		int index = dates.indexOf(date);
		Projection projection;
		synchronized (positions) {
			positions.set(index, newValue.getAmount());
			projection = new Projection(account, version++, positions);
		}
		Set<Update<Projection>> insertions = Collections.singleton(new Update<Projection>(null, projection));
		view.update(insertions, new ArrayList<Update<Projection>>(), new ArrayList<Update<Projection>>());
	}

}
