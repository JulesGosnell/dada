package com.nomura.cash;

import java.awt.GridBagLayout;
import java.awt.LayoutManager;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class GUI implements Runnable {

	protected final JFrame frame = new JFrame("Cash Sheet");
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JTable currencyTable = new JTable();
	protected final JScrollPane currencyScrollPane = new JScrollPane(currencyTable);
	protected final JTable accountTable = new JTable();
	protected final JScrollPane accountScrollPane = new JScrollPane(accountTable);
	protected final JTable tradeTable = new JTable();
	protected final JScrollPane tradeScrollPane = new JScrollPane(tradeTable);
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountScrollPane, tradeScrollPane);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencyScrollPane, splitPane2);
	
	protected TableModel tableModel = new AbstractTableModel() {
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 10;
		}
		
		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 10;
		}
	};

	public void run() {
		currencyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		currencyTable.setModel(tableModel);
		accountTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		accountTable.setModel(tableModel);
		tradeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tradeTable.setModel(tableModel);
		panel.setLayout(layout);
		panel.add(splitPane1);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new GUI());
	}
}
