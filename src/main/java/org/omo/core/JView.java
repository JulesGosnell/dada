/**
 * 
 */
package org.omo.core;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class JView extends JScrollPane {
	protected final JTable table; 
	protected TableModel model;

	public JView(TableModel model) {
		super(new JTable(model));
		this.model = model;
		table = (JTable)((JComponent)getComponent(0)).getComponent(0); // is it really this hard ?
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//Dimension oldSize = table.getPreferredScrollableViewportSize();
		//Dimension preferredSize = table.getPreferredSize();
		//int width = Math.min(preferredSize.width, oldSize.width);
		//int height= Math.min(preferredSize.height, oldSize.height);
		//Dimension newSize = new Dimension(width, height);
		//table.setPreferredScrollableViewportSize(newSize);
	}

	public void setModel(TableModel model) {
		this.model = model;
		table.setModel(model);
	}
	
	public JTable getTable() {
		return table;
	}
}