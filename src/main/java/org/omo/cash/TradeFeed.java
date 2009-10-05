package org.omo.cash;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Feed;
import org.omo.core.IntrospectiveMetadata;
import org.omo.core.Metadata;
import org.omo.core.Range;

public class TradeFeed extends Feed<Integer, Trade> {
	
	private static final Log log = LogFactory.getLog(TradeFeed.class);
	
	private static /* final */ Metadata<Integer, Trade> metadata;
	
	static {
		try {
			metadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		} catch (Exception e) {
			log.fatal("could not build metadata for TradeFeed", e);
		}
	}
	
	public TradeFeed(String name, int partition, Range<Integer> range, Range<Date> dateRange, Range<Integer> accountRange, Range<Integer> currencyRange, long delay) {
		super(name, metadata, range, delay, new TradeFeedStrategy(dateRange, accountRange, currencyRange));
	}
	
}
