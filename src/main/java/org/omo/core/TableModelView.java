package org.omo.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableModelView<K, V> extends AbstractTableModel implements View<K, V> {

	private final Log log = LogFactory.getLog(getClass());
	
	private Metadata<K, V> metadata;
	private final ConcurrentSkipListMap<K, V> map = new ConcurrentSkipListMap<K, V>();
	
	public void setMetadata(Metadata<K, V> metadata) {
		this.metadata = metadata;
	}

	// Listener
	
	@Override
	public void insert(V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(V oldValue, V newValue) {
		log.trace("UPDATE("+newValue+")");
		
		K key = metadata.getKey(newValue);
		map.put(key, newValue);
		int index = map.headMap(key).size();
		fireTableRowsUpdated(index, index);
	}
	
	@Override
	public void delete(K key) {
		throw new UnsupportedOperationException("NYI");
	}

	// TableModel

	
	// Listener
	
	@Override
	public void batch(Collection<V> insertions, Collection<Update<V>> updates, Collection<K> deletions) {
		//log.trace("UPDATE("+upsertions+")");
		if (insertions != null)
			for (V insertion : insertions) {
				K key = metadata.getKey(insertion);
				map.put(key, insertion);
				int index = map.headMap(key).size();
				fireTableRowsInserted(index, index);
			}
		if (updates != null)
			for (Update<V> update : updates) {
				V newValue = update.getNewValue();
				K key = metadata.getKey(newValue);
				map.put(key, newValue);
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
		V value = (V)map.values().toArray()[rowIndex]; // yikes !!
		return metadata.getAttributeValue(value, columnIndex);
	}

}
