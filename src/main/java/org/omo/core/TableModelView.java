package org.omo.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class TableModelView<K, V> extends AbstractTableModel implements View<K, V> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final CountDownLatch latch = new CountDownLatch(1);
	private Metadata<K, V> metadata;
	private final ConcurrentSkipListMap<K, V> map = new ConcurrentSkipListMap<K, V>();
	
	public void setMetadata(Metadata<K, V> metadata) {
		this.metadata = metadata;
		latch.countDown();
	}

	public Metadata<K, V> getMetadata() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return metadata;
	}
	
	// TableModel
	
	// Listener
	
	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		logger.trace("update: {}", insertions);
		Metadata<K, V> metadata = getMetadata();
		for (Update<V> insertion : insertions) {
			V newValue = insertion.getNewValue();
			K key = metadata.getKey(newValue);
			map.put(key, newValue);
			int index = map.headMap(key).size();
			fireTableRowsInserted(index, index);
		}
		for (Update<V> update : updates) {
			V newValue = update.getNewValue();
			K key = metadata.getKey(newValue);
			V oldValue = map.get(key);
			if (oldValue != null /* || oldValue.getV */ ) {
				map.put(key, newValue);
				int index = map.headMap(key).size();
				fireTableRowsInserted(index, index);
			}
		}
		for (Update<V> deletion : deletions) {
			throw new UnsupportedOperationException("deletion - NYI");
		}
	}

	protected String columnNames[] = new String[]{"id", "version"};

	@Override
	public String getColumnName(int columnIndex) {
		return getMetadata().getAttributeNames().get(columnIndex);
	}
	
	@Override
	public int getColumnCount() {
		return getMetadata().getAttributeNames().size();
	}

	@Override
	public int getRowCount() {
		return map.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		V value = (V)map.values().toArray()[rowIndex]; // yikes !!
		return getMetadata().getAttributeValue(value, columnIndex);
	}

}
