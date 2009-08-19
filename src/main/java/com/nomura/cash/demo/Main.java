package com.nomura.cash.demo;

// TODO:
// what should we do about id2Index ?
// should separate Server and Client to understand issues ?
// make Demo main class


import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.nomura.cash.Account;
import com.nomura.cash.AccountImpl;
import com.nomura.cash.AccountManager;
import com.nomura.cash.AccountManagerImpl;
import com.nomura.cash.Listener;
import com.nomura.cash.Trade;
import com.nomura.cash.TradeImpl;

public class Main implements Runnable {

	protected class InitialTableModel extends AbstractTableModel {

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
		
	};

	protected class JSection extends JScrollPane {
		protected final JTable table; 
		protected TableModel model;

		public JSection() {
			super(new JTable(new InitialTableModel()));
			table = (JTable)((JComponent)getComponent(0)).getComponent(0); // is it really this hard ?   
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			model = table.getModel();
			Dimension oldSize = table.getPreferredScrollableViewportSize();
			Dimension preferredSize = table.getPreferredSize();
			int width = Math.min(preferredSize.width, oldSize.width);
			int height= Math.min(preferredSize.height, oldSize.height);
			Dimension newSize = new Dimension(width, height);
			table.setPreferredScrollableViewportSize(newSize);
		}

		public void setModel(TableModel model) {
			this.model = model;
			table.setModel(model);
		}
	}
	
	protected final JSection currencySection = new JSection();
	protected final JSection accountSection = new JSection();
	protected final JSection tradeSection = new JSection();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountSection, tradeSection);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencySection, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Cash Sheet");

	protected final Account account = new AccountImpl(0);
	protected final AccountManager accountManager = new AccountManagerImpl(account);
	
	public void run() {
		panel.add(splitPane2);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		// plug accountManager into tradeSection...
		final AbstractTableModel tradeModel = new AbstractTableModel() {

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
		};
		tradeSection.setModel(tradeModel);

		accountManager.register(new Listener<Trade>() {
			
			@Override
			public void update(Trade oldValue, Trade newValue) {
				if (oldValue==null)
					tradeModel.fireTableRowsInserted(newValue.getId(), newValue.getId());
				else
					tradeModel.fireTableRowsUpdated(newValue.getId(), newValue.getId());
			}
		});
		
		final AbstractTableModel accountModel = new AbstractTableModel() {
			
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
		};

		accountSection.setModel(accountModel);
		
		accountManager.registerPositionListener(new Listener<Integer>() {
			
			@Override
			public void update(Integer oldValue, Integer newValue) {
				accountModel.fireTableRowsUpdated(0, 0);
			}
		});

		new Thread(new Runnable() {
			public void run() {
				try {
					// run some trades into accountManager...
					for (int id=0; id<100; id++) {
						accountManager.update(new TradeImpl(id, 100));
						Thread.sleep(100);
					}
					// keep them updating...
					while (true) {
						int id = (int)(Math.random()*100);
						int amount = (int)(Math.random()*100);
						accountManager.update(new TradeImpl(id, amount));
						Thread.sleep(50);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Main());
	}
}
