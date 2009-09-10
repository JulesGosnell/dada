package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.omo.cash.Trade;
import org.omo.core.View;

// should just be an IdentityView

public class TradePartitioner implements View<Integer, Trade> {

	private final List<TradePartition> tradePartitions;
	private final int numPartitions;
	
	public TradePartitioner(List<TradePartition> tradePartitions) {
		this.tradePartitions = tradePartitions;
		numPartitions = this.tradePartitions.size();
	}
	
	@Override
	public void delete(Collection<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(Integer deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void upsert(Collection<Trade> upsertions) {
		List<Trade>[] tmp = new List[numPartitions];
		for (int partition=0; partition<numPartitions; partition++)
			tmp[partition] = new ArrayList<Trade>();
		for (Trade upsertion : upsertions)
			tmp[upsertion.getId()%numPartitions].add(upsertion);
		for (int partition=0; partition<numPartitions; partition++) {
			TradePartition tradePartition = tradePartitions.get(partition);
			List<Trade> upsertions2 = tmp[partition];
			if (upsertions2.size()>0)
				tradePartition.upsert(upsertions2);
		}
	}

	@Override
	public void upsert(Trade upsertion) {
		int partition = upsertion.getId()%numPartitions;
		tradePartitions.get(partition).upsert(upsertion);
	}

}
