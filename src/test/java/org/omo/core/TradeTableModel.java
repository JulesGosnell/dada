package org.omo.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.cash.Trade;
import org.omo.core.View;

public class TradeTableModel extends AbstractTableModel implements View<Integer, Trade>, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	
	private TreeMap<Integer, Trade> trades = new TreeMap<Integer, Trade>();
	
	// Listener
	
	@Override
	public void upsert(Collection<Trade> upsertions) {
		log.info("UPDATE("+upsertions+")");
		for (Trade upsertion : upsertions) {
			int id = upsertion.getId();
			trades.put(id, upsertion);
			int index = trades.headMap(id).size();
			fireTableRowsUpdated(index, index);
		}
	}

	@Override
	public void upsert(Trade upsertion) {
		log.info("UPDATE("+upsertion+")");
		int id = upsertion.getId();
		trades.put(id, upsertion);
		int index = trades.headMap(id).size();
		fireTableRowsUpdated(index, index);
	}
	
	@Override
	public void delete(Collection<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(Integer deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	// TableModel

	
	protected String columnNames[] = new String[]{"id", "version"};

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}
	
	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public int getRowCount() {
		return trades.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Trade trade = (Trade)trades.values().toArray()[rowIndex]; // yikes !!
		
		switch (columnIndex) {

		case 0: return trade.getId();
		case 1: return trade.getVersion();

		}
		
		throw new IllegalArgumentException("columnIndex out of bounds: " + columnIndex);
	}

}
