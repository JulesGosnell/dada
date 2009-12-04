package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.omo.core.AbstractModel;
import org.omo.core.Update;
import org.omo.core.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: to lock or to copy-on-write ?

public class CurrencyAmountAggregator extends AbstractModel<Date, CurrencyTotal> implements View< Integer, Trade> {

	private final Collection<Update<CurrencyTotal>> empty = new ArrayList<Update<CurrencyTotal>>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Date date;
	private final int currency;

	private BigDecimal aggregate = BigDecimal.ZERO;
	private int version; // TODO: needs to come from our Model, so if we go, it is not lost...
	
	public CurrencyAmountAggregator(String name, Date date, int currency) {
		super(name, null);
		this.date = date;
		this.currency = currency;
	}

	public synchronized void update(Collection<Update<Trade>> insertions, Collection<Update<Trade>> updates, Collection<Update<Trade>> deletions) {
		logger.debug("insertion: size={}", insertions.size());
		logger.debug("update   : size={}", updates.size());
		logger.debug("deletion : size={}", deletions.size());
		CurrencyTotal oldValue = new CurrencyTotal(date, version, currency, aggregate);
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
		CurrencyTotal newValue = new CurrencyTotal(date, ++version, currency, aggregate);
		List<Update<CurrencyTotal>> updatesOut = Collections.singletonList(new Update<CurrencyTotal>(oldValue, newValue));
		notifyUpdates(empty, updatesOut, empty);
	}

	@Override
	protected Collection<CurrencyTotal> getData() {
		return Collections.singleton(new CurrencyTotal(date, version, currency, aggregate));
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	
}
