package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.omo.core.AbstractModel;
import org.omo.core.Aggregator;
import org.omo.core.DateRange;
import org.omo.core.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionAggregator extends AbstractModel<Integer, Projection> implements Aggregator<Projection, Date, AccountTotal> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Collection<Update<Projection>> empty = new ArrayList<Update<Projection>>(0); 
	private final List<BigDecimal> positions;
	private final List<Date> dates;
	private final int account;

	private int version;
	
	public ProjectionAggregator(String name, DateRange dateRange, int account) {
		super(name, null);
		this.account = account;
		dates = new ArrayList<Date>(dateRange.getValues()); // TODO: clumsy - but enables index lookup - slow - should be a Map ?
		positions = new ArrayList<BigDecimal>(dates.size());
		for (Date date : dates) {
			positions.add(BigDecimal.ZERO);
		}
	}
	
	@Override
	public void update(Collection<Update<AccountTotal>> insertions, Collection<Update<AccountTotal>> updates, Collection<Update<AccountTotal>> deletions) {
		logger.debug("insert: size={}", insertions.size());
		logger.debug("update: size={}", updates.size());
		logger.debug("remove: size={}", deletions.size());
		Projection oldValue2 = new Projection(account, version, new ArrayList<BigDecimal>(positions));

		for (Update<AccountTotal> insertion : deletions) {
			AccountTotal newValue = insertion.getNewValue();
			Date date = newValue.getId();
			int index = dates.indexOf(date);
			synchronized (positions) {
				positions.set(index, newValue.getAmount());
			}
		}
		for (Update<AccountTotal> update : updates) {
			AccountTotal oldValue = update.getOldValue();
			AccountTotal newValue = update.getNewValue();
			Date date = newValue.getId();
			int index = dates.indexOf(date);
			synchronized (positions) {
				positions.set(index, newValue.getAmount());
			}
		}
		for (Update<AccountTotal> deletion : deletions) {
			AccountTotal oldValue = deletion.getOldValue();
			Date date = oldValue.getId();
			int index = dates.indexOf(date);
			synchronized (positions) {
				positions.set(index, BigDecimal.ZERO);
			}
		}
		
		Projection newValue2 = new Projection(account, ++version, new ArrayList<BigDecimal>(positions));
		Set<Update<Projection>> updatesOut = Collections.singleton(new Update<Projection>(oldValue2, newValue2));
		notifyUpdates(empty, updatesOut, empty); 
	}

	@Override
	public Projection getAggregate() {
		return new Projection(account, version, new ArrayList<BigDecimal>(positions));
	}

	@Override
	protected Collection<Projection> getData() {
		return Collections.singleton(new Projection(account, version, new ArrayList<BigDecimal>(positions)));
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

}
