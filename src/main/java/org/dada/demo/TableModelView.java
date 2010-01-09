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

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;

import javax.swing.table.AbstractTableModel;

import org.dada.core.Metadata;
import org.dada.core.Update;
import org.dada.core.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			if (oldValue != null /* || oldValue.getV */) {
				map.put(key, newValue);
				int index = map.headMap(key).size();
				fireTableRowsInserted(index, index);
			}
		}

		if (deletions.size() > 0)
			throw new UnsupportedOperationException("deletion - NYI");
	}

	protected String[] columnNames = new String[]{"id", "version"};

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

	@SuppressWarnings("unchecked")
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		V value = (V) map.values().toArray()[rowIndex]; // TODO: yikes !!
		return getMetadata().getAttributeValue(value, columnIndex);
	}

}
