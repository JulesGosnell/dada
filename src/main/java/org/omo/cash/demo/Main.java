package org.omo.cash.demo;

// TODO:
// what should we do about id2Index ?
// should separate Server and Client to understand issues ?
// make Demo main class


import java.awt.LayoutManager;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.omo.cash.Account;
import org.omo.cash.AccountImpl;
import org.omo.cash.AccountManager;
import org.omo.cash.AccountManagerImpl;
import org.omo.cash.Listener;
import org.omo.cash.Trade;
import org.omo.cash.TradeImpl;


public class Main implements Runnable {

	// Client
	protected final JView currencyView = new JView();
	protected final JView accountView = new JView();
	protected final JView tradeView = new JView();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountView, tradeView);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencyView, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Cash Sheet");
	
	// Server
	protected final Account account = new AccountImpl(0);
	protected final AccountManager accountManager = new AccountManagerImpl(account);
	
	public void run() {
		panel.add(splitPane2);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		// plug accountManager into tradeView...
		final AbstractTableModel tradeModel = new TradeModel(accountManager);
		tradeView.setModel(tradeModel);

		accountManager.register(new Listener<Trade>() {
			
			@Override
			public void update(Trade oldValue, Trade newValue) {
				if (oldValue==null)
					tradeModel.fireTableRowsInserted(newValue.getId(), newValue.getId());
				else
					tradeModel.fireTableRowsUpdated(newValue.getId(), newValue.getId());
			}
		});
		
		final AbstractTableModel accountModel = new AccountModel(accountManager);

		accountView.setModel(accountModel);
		
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
