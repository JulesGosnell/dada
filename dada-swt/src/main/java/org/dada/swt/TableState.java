package org.dada.swt;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.TableItem;

public class TableState<K, V> {
		
	//private Map<K, V> extant = new HashMap<K, V>();
	//private Map<K, V> extinct = new HashMap<K, V>();

	public Map<K, TableItem> primaryKeyToTableItem = new HashMap<K, TableItem>();

}
