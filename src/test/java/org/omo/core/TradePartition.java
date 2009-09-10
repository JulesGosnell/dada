package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.omo.core.AbstractModel;
import org.omo.core.View;

public class TradePartition extends AbstractModel<Integer, Trade> implements View<Integer, Trade> {

	private final int partitionNumber;
	
	TradePartition(String name, int partitionNumber) {
		super(name);
		this.partitionNumber = partitionNumber; 
	}

	// Lifecycle
	
	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	// Model
	
	protected Collection<Trade> getData() {
		synchronized (trades) {
			return new ArrayList<Trade>(trades.values());
		}
	}
	
	@Override
	public void delete(Collection<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(Integer deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	protected final Map<Integer, Trade> trades = new HashMap<Integer, Trade>();
	
	@Override
	public void upsert(Collection<Trade> upsertions) {
		synchronized (trades) {
			for (Trade upsertion : upsertions)
				trades.put(upsertion.getId(), upsertion);
		}
		notifyUpsertion(upsertions);
	}

	@Override
	public void upsert(Trade upsertion) {
		synchronized (trades) {
			trades.put(upsertion.getId(), upsertion);
		}
		notifyUpsertion(upsertion);
	}

}
