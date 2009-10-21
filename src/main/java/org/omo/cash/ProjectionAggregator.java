package org.omo.cash;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.omo.core.Aggregator;
import org.omo.core.DateRange;
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

	@Override
	public synchronized void insert(Collection<AccountTotal> values) {
		for (AccountTotal value : values) {
			//log.info("insert: " + value);
			Date date = value.getId();
			int index = dates.indexOf(date);
			positions.set(index, value.getAmount());
		}
		Projection projection = new Projection(account, version++, positions);
		view.update(Collections.singleton(projection)); // TODO: do we want this inside sync block ?
	}

	@Override
	public void remove(AccountTotal value) {
		//log.info("remove: " + value);
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void update(AccountTotal oldValue, AccountTotal newValue) {
		//log.info("update: " + oldValue + " -> " + newValue);
		Date date = newValue.getId();
		int index = dates.indexOf(date);
		Projection projection;
		synchronized (positions) {
			positions.set(index, newValue.getAmount());
			projection = new Projection(account, version++, positions);
		}
		view.update(Collections.singleton(projection));
	}

}
