package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Feed<K, V> extends AbstractModel<K, V> {

	// Feed
	
	public interface Strategy<K, V> {
		
		K getKey(V item);
		V createNewItem(int counter);
		V createNewVersion(V original);
		
	}
	
	protected final Map<K, V> vs = new HashMap<K, V>();
	protected final int numTrades;
	protected final long delay;
	protected final Strategy<K, V> strategy;
	protected final Timer timer = new Timer();
	protected final TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			int id = (int)(Math.random()*numTrades);
			V oldTrade = vs.get(id);
			// Trade newTrade = new Trade(id, oldTrade.getVersion()+1);
			V newTrade = strategy.createNewVersion(oldTrade);
			vs.put(strategy.getKey(newTrade), newTrade);
			notifyInsertion(newTrade);
		}
	};
	
	public Feed(String name, int numTrades, long delay, Strategy<K, V> feedStrategy) {
		super(name);
		this.numTrades = numTrades;
		this.delay = delay;
		this.strategy = feedStrategy;
	}

	// Lifecycle
	
	@Override
	public void start() {
		for (int i=0 ;i<numTrades; i++) {
			V item = strategy.createNewItem(i);
			vs.put(strategy.getKey(item), item);
		}
		notifyBatch(vs.values(), null, null);
		timer.scheduleAtFixedRate(task, 0, delay);
	}
	
	@Override
	public void stop() {
		timer.cancel();
	}
	
	// Model
	
	protected Collection<V> getData() {
		return new ArrayList<V>(vs.values());
	}
	
}
