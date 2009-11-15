package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.omo.core.AbstractModel;
import org.omo.core.Aggregator;
import org.omo.core.Update;
import org.omo.core.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator extends AbstractModel<Date, AccountTotal> implements Aggregator<BigDecimal, Integer, Trade> {

	private final Collection<Update<AccountTotal>> empty = new ArrayList<Update<AccountTotal>>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Date date;
	private final int account;

	private BigDecimal aggregate = BigDecimal.ZERO;
	private int version; // TODO: needs to come from our Model, so if we go, it is not lost...
	
	public AmountAggregator(String name, Date date, int account) {
		super(name, null);
		this.date = date;
		this.account = account;
	}

	public synchronized void update(Collection<Update<Trade>> insertions, Collection<Update<Trade>> updates, Collection<Update<Trade>> deletions) {
		logger.debug("insertion: size={}", insertions.size());
		logger.debug("update   : size={}", updates.size());
		logger.debug("deletion : size={}", deletions.size());
		AccountTotal oldValue = new AccountTotal(date, version, account, aggregate);
		BigDecimal newAggregate = aggregate;
		for (Update<Trade> insertion : insertions) {
			newAggregate = newAggregate.add(insertion.getNewValue().getAmount());
		}
		for (Update<Trade> update : updates) {
			newAggregate = newAggregate.subtract(update.getOldValue().getAmount());
			newAggregate = newAggregate.add(update.getNewValue().getAmount());
		}
		for (Update<Trade> deletion : deletions) {
			newAggregate = newAggregate.subtract(deletion.getOldValue().getAmount());
		}
		aggregate = newAggregate;
		AccountTotal newValue = new AccountTotal(date, ++version, account, aggregate);
		List<Update<AccountTotal>> updatesOut = Collections.singletonList(new Update<AccountTotal>(oldValue, newValue));
		notifyUpdates(empty, updatesOut, empty);
	}

	@Override
	public synchronized BigDecimal getAggregate() {
		return aggregate;
	}
	
	@Override
	protected Collection<AccountTotal> getData() {
		return Collections.singleton(new AccountTotal(date, version, account, aggregate));
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	
}
