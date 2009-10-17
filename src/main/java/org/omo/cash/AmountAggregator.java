package org.omo.cash;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Aggregator;
import org.omo.core.View;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator implements Aggregator<BigDecimal, Trade> {

	private final Log log = LogFactory.getLog(getClass());
	
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
	
	public synchronized void insert(Trade value) {
		aggregate = aggregate.add(value.getAmount());
		log.trace(name + ": insert: " + value);
		view.update(Collections.singletonList(new AccountTotal(date, version++, account, aggregate)));
	}
	
	public synchronized void update(Trade oldValue, Trade newValue) {
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		log.trace(name + ": update: " + oldValue + " -> " + newValue);
		view.update(Collections.singletonList(new AccountTotal(date, version++, account, aggregate)));
	}
	
	public synchronized void remove(Trade value) {
		aggregate = aggregate.subtract(value.getAmount());
		log.trace(name + ": remove: " + value);
		view.update(Collections.singletonList(new AccountTotal(date, version++, account, aggregate)));
	}
	
}
