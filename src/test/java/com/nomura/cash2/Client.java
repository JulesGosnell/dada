package com.nomura.cash2;

import java.awt.LayoutManager;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.nomura.cash.demo.JView;
import com.nomura.consensus.jms.QueueFactory;
import com.nomura.consensus.jms.RemotingFactory;

public class Client implements Runnable, Listener {

	protected final JView currencyView = new JView();
	protected final JView accountView = new JView();
	protected final JView tradeView = new JView();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountView, tradeView);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencyView, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Cash Sheet");
	
	public void run() {
		panel.add(splitPane2);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		
		
		// plug accountManager into tradeView...
//		final AbstractTableModel tradeModel = new TradeModel(accountManager);
//		tradeView.setModel(tradeModel);

//		accountManager.register(new Listener<Trade>() {
//			
//			@Override
//			public void update(Trade oldValue, Trade newValue) {
//				if (oldValue==null)
//					tradeModel.fireTableRowsInserted(newValue.getId(), newValue.getId());
//				else
//					tradeModel.fireTableRowsUpdated(newValue.getId(), newValue.getId());
//			}
//		});


	}
	
	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		QueueFactory queueFactory = new QueueFactory();
		int timeout = 5000;
		RemotingFactory<View> factory = new RemotingFactory<View>(session, View.class, queueFactory, timeout);
		View view = factory.createSynchronousClient();
		view.addElementListener(null);
		
		SwingUtilities.invokeLater(new Client());
	}

	// Listener...
	
	@Override
	public void update(List updates) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(Object update) {
		// TODO Auto-generated method stub
		
	}
	
}
