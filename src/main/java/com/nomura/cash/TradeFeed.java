package org.omo.cash;

import java.util.Date;

import org.omo.core.Feed;
import org.omo.core.IntrospectiveMetadata;
import org.omo.core.Metadata;
import org.omo.core.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeFeed extends Feed<Integer, Trade> {
	
	private static final Logger logger = LoggerFactory.getLogger(TradeFeed.class);
	
	private static /* final */ Metadata<Integer, Trade> metadata;
	
	static {
		try {
			metadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		} catch (Exception e) {
			logger.error("could not build metadata for TradeFeed", e);
		}
	}
	
	public TradeFeed(String name, int partition, Range<Integer> range, Range<Date> dateRange, Range<Integer> accountRange, Range<Integer> currencyRange, long delay) {
		super(name, metadata, range, delay, new TradeFeedStrategy(dateRange, accountRange, currencyRange));
	}
	
}
