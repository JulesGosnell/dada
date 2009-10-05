package org.omo.cash;

import java.math.BigDecimal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Aggregator;

// TODO: to lock or to copy-on-write ?

public class AmountAggregator implements Aggregator<BigDecimal, Trade> {

	private final Log log = LogFactory.getLog(getClass());
	private volatile BigDecimal aggregate = new BigDecimal(0);
	
	private final int day;
	private final int account;
	
	public AmountAggregator(int day, int account) {
		this.day = day;
		this.account = account;
	}

	public BigDecimal getAggregate() {
		return aggregate;
	}
	
	public void insert(Trade value) {
		aggregate = aggregate.add(value.getAmount());
		log.debug("total["+day+","+account+"]: " + aggregate);
	}
	
	public void update(Trade oldValue, Trade newValue) {
		aggregate = aggregate.add(newValue.getAmount()).subtract(oldValue.getAmount());
		log.debug("total["+day+","+account+"]: " + aggregate);
	}
	
	public void remove(Trade value) {
		aggregate = aggregate.subtract(value.getAmount());
		log.debug("total["+day+","+account+"]: " + aggregate);
	}
	
}
