package com.nomura.cash2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestView<Input> extends AbstractTableModel implements View<Input>, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	
	private TreeMap<Integer, Input> trades = new TreeMap<Integer, Input>();
	
	// Listener
	
	@Override
	public void upsert(Collection<Input> upsertions) {
		log.info("TEST LISTENER: UPDATE("+upsertions+")");
		for (Input upsertion : upsertions) {
			int id = ((Trade)upsertion).getId();
			trades.put(id, upsertion);
			fireTableRowsUpdated(id, id);
		}
	}

	@Override
	public void upsert(Input upsertion) {
		log.info("TEST LISTENER: UPDATE("+upsertion+")");
		int id = ((Trade)upsertion).getId();
		trades.put(id, upsertion);
		fireTableRowsUpdated(id, id);
	}
	
	@Override
	public void delete(Collection<Integer> deletions) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void delete(int deletion) {
		throw new UnsupportedOperationException("NYI");
	}

	// TableModel

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		return trades.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return trades.get(rowIndex);
	}

}
