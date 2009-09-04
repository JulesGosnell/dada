package com.nomura.cash2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TradePartition extends AbstractModel<Trade> implements View<Trade> {

	private final int partitionNumber;
	
	TradePartition(int partitionNumber) {
		this.partitionNumber = partitionNumber; 
	}
	
	@Override
	public void registerView(View<Trade> view) {
		view.upsert(new ArrayList<Trade>(trades.values()));
		super.registerView(view);
	}
	
	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void delete(Collection<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(int deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	protected final Map<Integer, Trade> trades = new HashMap<Integer, Trade>();
	
	@Override
	public void upsert(Collection<Trade> upsertions) {
		for (Trade upsertion : upsertions)
			trades.put(upsertion.getId(), upsertion);
		notifyUpsertion(upsertions);
	}

	@Override
	public void upsert(Trade upsertion) {
		trades.put(upsertion.getId(), upsertion);
		notifyUpsertion(upsertion);
	}

}
