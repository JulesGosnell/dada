package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.omo.core.AbstractModel;

public class TradeGenerator extends AbstractModel<Trade> {

	// TradeGenerator
	
	protected Map<Integer, Trade> trades = new HashMap<Integer, Trade>();
	protected int numTrades;
	protected long delay;
	protected Timer timer = new Timer();
	protected TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			int id = (int)(Math.random()*numTrades);
			Trade oldTrade = trades.get(id);
			Trade newTrade = new Trade(id, oldTrade.getVersion()+1);
			trades.put(id, newTrade);
			notifyUpsertion(newTrade);
		}
	};
	
	public TradeGenerator(String name, int numTrades, long delay) {
		super(name);
		this.numTrades = numTrades;
		this.delay = delay;
	}

	// Lifecycle
	
	@Override
	public void start() {
		for (int i=0 ;i<numTrades; i++)
			trades.put(i, new Trade(i, 0));
		notifyUpsertion(trades.values());
		timer.scheduleAtFixedRate(task, 0, delay);
	}
	
	@Override
	public void stop() {
		timer.cancel();
	}
	
	// Model
	
	protected Collection<Trade> getData() {
		return new ArrayList<Trade>(trades.values());
	}
	
}

