package org.omo.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableModelView<InputKey, InputValue> extends AbstractTableModel implements View<InputKey, InputValue> {

	private final Log log = LogFactory.getLog(getClass());
	
	private Metadata<InputKey, InputValue> metadata;
	private final ConcurrentSkipListMap<InputKey, InputValue> map = new ConcurrentSkipListMap<InputKey, InputValue>();
	
	public void setMetadata(Metadata<InputKey, InputValue> metadata) {
		this.metadata = metadata;
	}

	// Listener
	
	@Override
	public void insert(InputValue value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(InputValue value) {
		log.trace("UPDATE("+value+")");
		InputKey key = metadata.getKey(value);
		map.put(key, value);
		int index = map.headMap(key).size();
		fireTableRowsUpdated(index, index);
	}
	
	@Override
	public void delete(InputKey key) {
		throw new UnsupportedOperationException("NYI");
	}

	// TableModel

	
	// Listener
	
	@Override
	public void batch(Collection<InputValue> insertions, Collection<InputValue> updates, Collection<InputKey> deletions) {
		//log.trace("UPDATE("+upsertions+")");
		if (insertions != null)
			for (InputValue insertion : insertions) {
				InputKey key = metadata.getKey(insertion);
				map.put(key, insertion);
				int index = map.headMap(key).size();
				fireTableRowsInserted(index, index);
			}
		if (updates != null)
			for (InputValue update : updates) {
				InputKey key = metadata.getKey(update);
				map.put(key, update);
				int index = map.headMap(key).size();
				fireTableRowsUpdated(index, index);
			}
		// TODO: handle deletions
	}

	protected String columnNames[] = new String[]{"id", "version"};

	@Override
	public String getColumnName(int columnIndex) {
		return metadata.getAttributeNames().get(columnIndex);
	}
	
	@Override
	public int getColumnCount() {
		return metadata.getAttributeNames().size();
	}

	@Override
	public int getRowCount() {
		return map.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		InputValue value = (InputValue)map.values().toArray()[rowIndex]; // yikes !!
		return metadata.getAttributeValue(value, columnIndex);
	}

}
