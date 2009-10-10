package org.omo.cash;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Aggregator;
import org.omo.core.View;

import edu.emory.mathcs.backport.java.util.Collections;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator implements Aggregator<BigDecimal, Trade> {

	private final Log log = LogFactory.getLog(getClass());
	
	private final String name;
	private final Date day;
	private final int account;
	private final View<Date, AccountTotal> view;

	private BigDecimal aggregate = new BigDecimal(0);
	private int version; // TODO: needs to come from our Model, so if we go, it in not lost...
	
	public AmountAggregator(String name, Date day, int account, View<Date, AccountTotal> view) {
		this.name = name;
		this.day = day;
		this.account = account;
		this.view = view;
	}

	public synchronized BigDecimal getAggregate() {
		return aggregate;
	}
	
	public synchronized void insert(Trade value) {
		aggregate = aggregate.add(value.getAmount());
		view.update(Collections.singletonList(new AccountTotal(account, version++, day, aggregate)));
	}
	
	public synchronized void update(Trade oldValue, Trade newValue) {
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		view.update(Collections.singletonList(new AccountTotal(account, version++, day, aggregate)));
	}
	
	public synchronized void remove(Trade value) {
		aggregate = aggregate.subtract(value.getAmount());
		view.update(Collections.singletonList(new AccountTotal(account, version++, day, aggregate)));
	}
	
}
