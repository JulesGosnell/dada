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

	private final Collection<Update<AccountTotal>> empty = new ArrayList<Update<AccountTotal>>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final String name;
	private final Date date;
	private final int account;
	private final View<Date, AccountTotal> view;

	private BigDecimal aggregate = new BigDecimal(0);
	private int version; // TODO: needs to come from our Model, so if we go, it is not lost...
	
	public AmountAggregator(String name, Date date, int account, View<Date, AccountTotal> view) {
		this.name = name;
		this.date = date;
		this.account = account;
		this.view = view;
	}

	public synchronized BigDecimal getAggregate() {
		return aggregate;
	}
	
	public synchronized void insert(Collection<Update<Trade>> insertions) {
		logger.debug("insert: size={}", insertions.size());
		AccountTotal oldValue = new AccountTotal(date, version, account, aggregate);
		for (Update<Trade> insertion : insertions) {
			aggregate = aggregate.add(insertion.getNewValue().getAmount());
		}
		AccountTotal newValue = new AccountTotal(date, ++version, account, aggregate);
		Collection<Update<AccountTotal>> updates = Collections.singletonList(new Update<AccountTotal>(oldValue, newValue));
		view.update(empty, updates, empty);
	}
	
	public synchronized void update(Trade oldValue, Trade newValue) {
		logger.debug("update: size={}", 1);
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		logger.trace(name + ": update: " + oldValue + " -> " + newValue); // TODO: slf4j-ise
		List<Update<AccountTotal>> insertions = Collections.singletonList(new Update<AccountTotal>(null, new AccountTotal(date, version++, account, aggregate)));
		view.update(insertions, empty, empty);
	}
	
	public synchronized void remove(Collection<Update<Trade>> deletions) {
		logger.debug("remove: size={}", deletions.size());
		AccountTotal oldValue = new AccountTotal(date, version, account, aggregate);
		for (Update<Trade> deletion : deletions) {
			aggregate = aggregate.subtract(deletion.getOldValue().getAmount());
		}
		AccountTotal newValue = new AccountTotal(date, ++version, account, aggregate);
		Collection<Update<AccountTotal>> updates = Collections.singleton(new Update<AccountTotal>(oldValue, newValue));
		view.update(empty, updates, empty);
	}
	
}
