package org.omo.cash;

import java.math.BigDecimal;

import org.omo.core.Aggregator;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator implements Aggregator<BigDecimal, Trade> {

	//private final Log log = LogFactory.getLog(getClass());
	private volatile BigDecimal aggregate = new BigDecimal(0);

	public BigDecimal getAggregate() {
		return aggregate;
	}
	
	public void insert(Trade value) {
		aggregate = aggregate.add(value.getAmount());
		//log.debug("total: " + aggregate);
	}
	
	public void update(Trade oldValue, Trade newValue) {
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		//log.debug("total: " + aggregate);
	}
	
	public void remove(Trade value) {
		aggregate = aggregate.subtract(value.getAmount());
		//log.debug("total: " + aggregate);
	}
	
}
