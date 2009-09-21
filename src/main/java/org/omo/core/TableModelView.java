package org.omo.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableModelView<InputKey, InputValue> extends AbstractTableModel implements View<InputKey, InputValue> {

	private final Log log = LogFactory.getLog(getClass());
	
	public interface Mapper<InputKey, InputValue> {
		InputKey getKey(InputValue value);
		Object getField(InputValue value, int index);
		List<String> getFieldNames();
	}

	private final Mapper<InputKey, InputValue> mapper;
	private final ConcurrentSkipListMap<InputKey, InputValue> map = new ConcurrentSkipListMap<InputKey, InputValue>();
	
	public TableModelView(Mapper<InputKey, InputValue> mapper) {
		this.mapper = mapper;
	}
	
	// Listener
	
	@Override
	public void insert(InputValue value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(InputValue value) {
		log.trace("UPDATE("+value+")");
		InputKey key = mapper.getKey(value);
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
		for (InputValue upsertion : updates) {
			InputKey key = mapper.getKey(upsertion);
			map.put(key, upsertion);
			int index = map.headMap(key).size();
			fireTableRowsUpdated(index, index);
		}
	}

	protected String columnNames[] = new String[]{"id", "version"};

	@Override
	public String getColumnName(int columnIndex) {
		return mapper.getFieldNames().get(columnIndex);
	}
	
	@Override
	public int getColumnCount() {
		return mapper.getFieldNames().size();
	}

	@Override
	public int getRowCount() {
		return map.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		InputValue value = (InputValue)map.values().toArray()[rowIndex]; // yikes !!
		return mapper.getField(value, columnIndex);
	}

}
