/**
 * 
 */
package com.nomura.cash.demo;

import javax.swing.table.AbstractTableModel;

import com.nomura.cash.AccountManager;

public class AccountModel extends AbstractTableModel {
	
	protected final AccountManager accountManager;
	
	public AccountModel(AccountManager accountManager) {
		this.accountManager = accountManager;
	}
	
	protected String columnNames[] = new String[]{"id", "amount", "excluded"};

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return accountManager.getId();
		case 1:
			return accountManager.getPosition();
		case 2:
			return accountManager.getExcluded();
		default:
			throw new IllegalArgumentException("columnIndex out of range: "+columnIndex+" > "+3);
		}
	}

	@Override
	public int getRowCount() {
		return 1;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}
}