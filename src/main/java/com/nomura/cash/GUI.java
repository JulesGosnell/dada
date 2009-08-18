package com.nomura.cash;

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

public class GUI implements Runnable {

	protected class CashTableModel extends AbstractTableModel {

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 10;
		}

		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 5;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		
	};

	protected class JSection extends JScrollPane {
		protected final JTable table; 
		protected final TableModel model;

		public JSection() {
			super(new JTable(new CashTableModel()));
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
	}
	
	protected final JSection currencySection = new JSection();
	protected final JSection accountSection = new JSection();
	protected final JSection tradeSection = new JSection();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountSection, tradeSection);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencySection, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Cash Sheet");
	
	public void run() {
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
