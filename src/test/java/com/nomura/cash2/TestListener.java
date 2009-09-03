package com.nomura.cash2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestListener<T> extends AbstractTableModel implements Listener<T>, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	
	private List<T> elements = new ArrayList<T>();
	
	// Listener
	
	@Override
	public void update(List<T> updates) {
		log.info("TEST LISTENER: UPDATE("+updates+")");
		elements.addAll(updates);
	}

	@Override
	public void update(T update) {
		log.info("TEST LISTENER: UPDATE("+update+")");
		elements.add(update);
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
