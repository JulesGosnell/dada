package com.nomura.cash2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TradeGenerator extends AbstractModel<Trade> {

	// Model
	
	@Override
	public void registerView(View<Trade> view) {
		view.upsert(new ArrayList<Trade>(trades.values()));
		super.registerView(view);
	}

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
	
	public TradeGenerator(int numTrades, long delay) {
		this.numTrades = numTrades;
		this.delay = delay;
	}

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
	
}

