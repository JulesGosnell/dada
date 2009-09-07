/**
 * 
 */
package org.omo.old.demo;

import javax.swing.table.AbstractTableModel;

public class InitialTableModel extends AbstractTableModel {

	@Override
	public int getColumnCount() {
		return 10;
	}

	@Override
	public int getRowCount() {
		return 10;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return null;
	}
	
}