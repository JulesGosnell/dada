/**
 * 
 */
package com.nomura.cash.demo;

import javax.swing.table.AbstractTableModel;

import com.nomura.cash.AccountManager;
import com.nomura.cash.Trade;

public class TradeModel extends AbstractTableModel {
	
	protected final AccountManager accountManager;
	
	public TradeModel(AccountManager accountManager) {
		this.accountManager = accountManager;
	}
	
	protected String columnNames[] = new String[]{"id", "amount", "excluded"};

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return accountManager.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Trade row = accountManager.fetch(rowIndex); // TODO: hack - need to be able to access trades by index...
		switch (columnIndex) {
		case 0:
			return row.getId();
		case 1:
			return row.getPosition();
		case 2:
			return row.getExcluded();
		default:
			throw new IllegalArgumentException("columnIndex out of range: "+columnIndex+" > "+columnNames.length);
		}
	}
}