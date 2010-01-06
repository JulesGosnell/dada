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

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableModel;

public class JView extends JScrollPane {
	protected final JTable table;
	protected TableModel model;

	public static class MyJTable extends JTable {
		MyJTable(TableModel model) {
			super(model);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			if (autoResizeMode != AUTO_RESIZE_OFF && getParent() instanceof JViewport) {
				return (((JViewport)getParent()).getWidth() > getPreferredSize().width);
			}
			return false;
		}
	}


	public JView(TableModel model) {
		super(new MyJTable(model));
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
