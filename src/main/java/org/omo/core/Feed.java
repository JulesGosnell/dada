package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Feed<K, V> extends AbstractModel<K, V> {

	// Feed
	
	public interface Strategy<K, V> {
		
		K getKey(V item);
		V createNewValue(int counter);
		V createNewVersion(V original);
		
	}
	
	protected final Map<K, V> vs = new HashMap<K, V>();
	protected final int numValues;
	protected final long delay;
	protected final Strategy<K, V> strategy;
	protected final Timer timer = new Timer();
	protected final TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			int id = (int)(Math.random()*numValues);
			V oldValue = vs.get(id);
			V newValue = strategy.createNewVersion(oldValue);
			vs.put(strategy.getKey(newValue), newValue);
			notifyUpdates(Collections.singleton(newValue));
		}
	};
	
	public Feed(String name, Metadata<K, V> metadata, int numValues, long delay, Strategy<K, V> feedStrategy) {
		super(name, metadata);
		this.numValues = numValues;
		this.delay = delay;
		this.strategy = feedStrategy;
	}

	// Lifecycle
	
	@Override
	public void start() {
		log.debug("creating " + numValues +" values...");
		for (int i=0 ;i<numValues; i++) {
			V item = strategy.createNewValue(i);
			vs.put(strategy.getKey(item), item);
		}
		log.debug("notifying " + numValues +" values...");
		notifyUpdates(vs.values());
		log.debug("starting timer...");
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
