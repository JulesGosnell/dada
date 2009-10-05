package org.omo.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.cash.Trade;

public class AggregatingModel extends AbstractModelView<Integer, List<BigDecimal>> {

	private final Log log = LogFactory.getLog(getClass());
	private List<List<BigDecimal>> data;

	public AggregatingModel(int numAccounts, int numDays) {
		super("Foo", null); // TODO: sort this out
		data = new ArrayList<List<BigDecimal>>(numAccounts);
		for (int a=0; a<numAccounts; a++) {
			ArrayList<BigDecimal> projection = new ArrayList<BigDecimal>();
			for (int d=0; d<numDays; d++)
				projection.add(new BigDecimal(0));
			data.add(projection);
		}
	}
	
	public Collection<List<BigDecimal>> getValues() {
		return data;
	}

	@Override
	public boolean deregisterView(View<Integer, List<BigDecimal>> view) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Registration<Integer, List<BigDecimal>> registerView(
			View<Integer, List<BigDecimal>> view) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}
	
	public void aggregate(int day, int account, AbstractModelView<Integer, Trade> model) {
		Aggregator<BigDecimal, Trade> aggregator = new AmountAggregator(day, account);
		model.register(aggregator);
		// TODO: what about initial refresh ?
	}
	
	protected class AmountAggregator implements Aggregator<BigDecimal, Trade> {

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
			// copy backbone
			List<List<BigDecimal>> newData = new ArrayList<List<BigDecimal>>();
			for (int i = 0; i < data.size(); i++) {
				newData.add(new ArrayList<BigDecimal>(data.get(i)));
			}
			// insert new value
			BigDecimal aggregate = data.get(day).get(account).add(value.getAmount());
			log.debug("total["+day+","+account+"]: " + aggregate);
			newData.get(day).set(account, aggregate);
			// update parent
			data = newData;
			// fire an event
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
}
