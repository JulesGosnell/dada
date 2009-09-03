package com.nomura.cash2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestListener<T> extends AbstractTableModel implements View<T>, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	
	private List<T> elements = new ArrayList<T>();
	
	// Listener
	
	@Override
	public void upsert(List<T> upsertions) {
		log.info("TEST LISTENER: UPDATE("+upsertions+")");
		elements.addAll(upsertions);
	}

	@Override
	public void upsert(T upsertion) {
		log.info("TEST LISTENER: UPDATE("+upsertion+")");
		elements.add(upsertion);
	}
	
	@Override
	public void delete(List<Integer> deletions) {
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
		return elements.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return elements.get(rowIndex);
	}

}
