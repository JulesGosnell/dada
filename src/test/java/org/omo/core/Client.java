package org.omo.core;

import java.awt.LayoutManager;
import java.util.Collection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Model;
import org.omo.core.View;
import org.omo.jms.RemotingFactory;
import org.omo.old.demo.JView;


public class Client implements Runnable {

	private static final Log LOG = LogFactory.getLog(Client.class);

	protected final JView currencyView = new JView();
	protected final JView accountView = new JView();
	protected final JView tradeView = new JView();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, accountView, tradeView);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currencyView, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Cash Sheet");

	private Session session;
	private int timeout;
	
	public void setModel1(TableModel model) {
		accountView.setModel(model);
	}
	public void setModel2(TableModel model) {
		tradeView.setModel(model);
	}
	public void setModel3(TableModel model) {
		currencyView.setModel(model);
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void run() {
		panel.add(splitPane2);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		// Client
		TradeTableModel guiModel1 = new TradeTableModel();
		try	{
			// create a client-side proxy for the Server
			Destination serverDestination = session.createQueue("Server.0.View");
			RemotingFactory<Model<Trade>> clientFactory = new RemotingFactory<Model<Trade>>(session, Model.class, serverDestination, timeout);
			Model<Trade> serverProxy = clientFactory.createSynchronousClient();

			// create a Client

			// create a client-side server to support callbacks on client
			Destination clientDestination = session.createQueue("Client.0.Listener");
			RemotingFactory<View<Trade>> serverFactory = new RemotingFactory<View<Trade>>(session, View.class, clientDestination, timeout);
			serverFactory.createServer(guiModel1);
			View<Trade> clientServer = serverFactory.createSynchronousClient();
			setModel1(guiModel1);
			
			// pass the client over to the server to attach as a listener..
			Collection<Trade> trades = serverProxy.registerView(clientServer);
			clientServer.upsert(trades);
			LOG.info("Client ready: "+clientDestination);
		} catch (JMSException e) {
			LOG.fatal(e);
		}
		TradeTableModel guiModel2 = new TradeTableModel();
		try	{
			// create a client-side proxy for the Server
			Destination serverDestination = session.createQueue("Server.1.View");
			RemotingFactory<Model<Trade>> clientFactory = new RemotingFactory<Model<Trade>>(session, Model.class, serverDestination, timeout);
			Model<Trade> serverProxy = clientFactory.createSynchronousClient();

			// create a Client

			// create a client-side server to support callbacks on client
			Destination clientDestination = session.createQueue("Client.1.Listener");
			RemotingFactory<View<Trade>> serverFactory = new RemotingFactory<View<Trade>>(session, View.class, clientDestination, timeout);
			serverFactory.createServer(guiModel2);
			View<Trade> clientServer = serverFactory.createSynchronousClient();
			setModel2(guiModel2);

			// pass the client over to the server to attach as a listener..
			Collection<Trade> trades = serverProxy.registerView(clientServer);
			clientServer.upsert(trades);
			LOG.info("Client ready: "+clientDestination);
		} catch (JMSException e) {
			LOG.fatal(e);
		}
	}
	
	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Client client = new Client();
		client.setSession(session);
		SwingUtilities.invokeLater(client);
	}

}