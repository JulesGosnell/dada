package org.dada.swt;


public class TableItemState<V> {

	public V datum;
	public TableCellState[] cells;
	
	public TableItemState(int numCells) {
		cells = new TableCellState[numCells];
	}
	
}
