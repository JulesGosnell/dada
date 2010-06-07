/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.table.AbstractTableModel;

import org.dada.core.Metadata;
import org.dada.core.Update;
import org.dada.core.View;
import org.dada.slf4j.Logger;
import org.dada.slf4j.LoggerFactory;

public class TableModelView<K, V> extends AbstractTableModel implements View<V> {

	private class IndexedMap<K, V> {
		
		private class Entry implements Map.Entry<K, V> {

			private final K key;
			private V value;
			
			private Entry(K key, V value) {
				this.key = key;
				this.value = value;
			}
			
			@Override
			public K getKey() {
				return key;
			}

			@Override
			public V getValue() {
				return value;
			}

			@Override
			public V setValue(V newValue) {
				V oldValue = this.value;
				this.value = newValue;
				return oldValue;
			}
			
		}
		
		private List<Entry> list = new ArrayList<Entry>();
		private Comparator<Entry> comparator = new Comparator<Entry>() {@Override public int compare(Entry o1, Entry o2) {return ((Comparable<K>)o1.getKey()).compareTo(o2.getKey());}};

		
		public int put(K key, V value) {
			Entry entry = new Entry(key, value);
			int i = Collections.binarySearch(list, entry, comparator);
			if (i > -1) {
				list.set(i, entry);
				return i;
			} else {
				int index = (i + 1) * -1;
				list.add(index, entry);
				return index;
			}
		}
		
		public int remove(K key) {
			Entry entry = new Entry(key, null);
			int i = Collections.binarySearch(list, entry, comparator);
			if (i > -1) {
				list.remove(i);
				return i;
			} else {
				return -1;
			}
		}
		
		public V get(int i) {
			return list.get(i).getValue();
		}

		public int size() {
			return list.size();
		}
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final CountDownLatch latch = new CountDownLatch(1);
	private Metadata<K, V> metadata;
	//private final ConcurrentSkipListMap<K, V> map = new ConcurrentSkipListMap<K, V>();
	private final IndexedMap<K, V> map = new IndexedMap<K, V>();
	private final String name;
	
	public TableModelView(String name) {
		this.name = name;
	}
	
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
	public synchronized void update(Collection<Update<V>> insertions, Collection<Update<V>> alterations, Collection<Update<V>> deletions) {
		logger.trace("{}:  input: insertions={}, alterations={}, deletions={}", name, insertions.size(), alterations.size(), deletions.size());

		// TODO - what about extinct values ?
		
		Metadata<K, V> metadata = getMetadata();
		for (Update<V> insertion : insertions) {
			V newValue = insertion.getNewValue();
			K key = metadata.getKey(newValue);
			int index = map.put(key, newValue);
			fireTableRowsInserted(index, index);
		}

		for (Update<V> update : alterations) {

			V newValue = update.getNewValue();
			K newKey = metadata.getKey(newValue);

			V oldValue = update.getOldValue();
			K oldKey = metadata.getKey(oldValue);

			int oldIndex;
			int newIndex;

			if (newKey.equals(oldKey)) {
				oldIndex = newIndex = map.put(newKey, newValue); 
			} else {
				newIndex = map.put(newKey, newValue);
				oldIndex = map.remove(oldKey);
			}
			
			if (oldIndex == newIndex)
				fireTableRowsUpdated(newIndex, newIndex);
			else {
				if (oldIndex > -1) fireTableRowsDeleted(oldIndex, oldIndex);
				fireTableRowsInserted(newIndex, newIndex);
			}
		}
		for (Update<V> deletion : deletions) {
			V value = deletion.getOldValue();
			K key = metadata.getKey(value);
			int index= map.remove(key);
			if (index > -1) fireTableRowsDeleted(index, index);
		}
		
		// TODO: can we collapse i/a/d into dirty blocks and make single UI updates ?
	}

	@Override
	public String getColumnName(int columnIndex) {
		return getMetadata().getAttributes().get(columnIndex).getKey().toString();
	}

	@Override
	public int getColumnCount() {
		return getMetadata().getAttributes().size();
	}

	@Override
	public synchronized int getRowCount() {
		return map.size();
	}

	@Override
	public synchronized Object getValueAt(int rowIndex, int columnIndex) {
		return getMetadata().getAttributeValue(map.get(rowIndex), columnIndex);
	}

}
