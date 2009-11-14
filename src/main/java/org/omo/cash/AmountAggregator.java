package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.omo.core.Aggregator;
import org.omo.core.Update;
import org.omo.core.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator implements Aggregator<BigDecimal, Trade> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final String name;
	private final Date date;
	private final int account;
	private final View<Date, AccountTotal> view;

	private BigDecimal aggregate = new BigDecimal(0);
	private int version; // TODO: needs to come from our Model, so if we go, it in not lost...
	
	public AmountAggregator(String name, Date date, int account, View<Date, AccountTotal> view) {
		this.name = name;
		this.date = date;
		this.account = account;
		this.view = view;
	}

	public synchronized BigDecimal getAggregate() {
		return aggregate;
	}
	
	public synchronized void insert(Collection<Trade> values) {
		logger.debug("insert: size={}", values.size());
		for (Trade value : values) {
			aggregate = aggregate.add(value.getAmount());
		}
		Collection<Update<AccountTotal>> insertions = Collections.singletonList(new Update<AccountTotal>(null, new AccountTotal(date, version++, account, aggregate)));
		view.update(insertions, new ArrayList<Update<AccountTotal>>(), new ArrayList<Update<AccountTotal>>());
	}
	
	public synchronized void update(Trade oldValue, Trade newValue) {
		logger.debug("update: size={}", 1);
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		logger.trace(name + ": update: " + oldValue + " -> " + newValue); // TODO: slf4j-ise
		List<Update<AccountTotal>> insertions = Collections.singletonList(new Update<AccountTotal>(null, new AccountTotal(date, version++, account, aggregate)));
		view.update(insertions, new ArrayList<Update<AccountTotal>>(), new ArrayList<Update<AccountTotal>>());
	}
	
	public synchronized void remove(Trade value) {
		logger.debug("renove: size={}", 1);
		aggregate = aggregate.subtract(value.getAmount());
		logger.trace("{}: remove: {}", name, value);
		List<Update<AccountTotal>> insertions = Collections.singletonList(new Update<AccountTotal>(null, new AccountTotal(date, version++, account, aggregate)));
		view.update(insertions, new ArrayList<Update<AccountTotal>>(), new ArrayList<Update<AccountTotal>>());
	}
	
}
