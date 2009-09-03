package com.nomura.cash2;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TradeGenerator implements Model<Trade> {

	// Model
	
	@Override
	public void registerView(View<Trade> view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deregisterView(View<Trade> view) {
		// TODO Auto-generated method stub

	}

	// TradeGenerator
	
	protected Map<Integer, Trade> trades = new HashMap<Integer, Trade>();
	protected int numTrades;
	protected long delay;
	protected Timer timer = new Timer();
	protected TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			int id = (int)Math.random()*numTrades;
			Trade trade = trades.get(id);
			trades.put(id, new  Trade(id, trade.getVersion()+1));
		}
	};
	public TradeGenerator(int numTrades, long delay) {
		this.numTrades = numTrades;
		this.delay = delay;
	}
	
	public void start() {
		for (int i=0 ;i<numTrades; i++)
			trades.put(i, new Trade(i, 0));
		//timer.schedule(task, delay)
	}
}
