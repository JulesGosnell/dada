package com.nomura.cash2;

import java.awt.LayoutManager;

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

import com.nomura.cash.demo.JView;
import com.nomura.consensus.jms.RemotingFactory;

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
	
	public void setModel(TableModel model) {
		currencyView.setModel(model);
		accountView.setModel(model);
		tradeView.setModel(model);
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
		TestListener guiModel = new TestListener();
		try	{
			// create a client-side proxy for the Server
			Destination serverDestination = session.createQueue("Server.View");
			RemotingFactory<View> clientFactory = new RemotingFactory<View>(session, View.class, serverDestination, timeout);
			View serverProxy = clientFactory.createSynchronousClient();

			// create a Client

			// create a client-side server to support callbacks on client
			Destination clientDestination = session.createQueue("Client.Listener");
			RemotingFactory<Listener> serverFactory = new RemotingFactory<Listener>(session, Listener.class, clientDestination, timeout);
			serverFactory.createServer(guiModel);
			Listener clientServer = serverFactory.createSynchronousClient();

			// pass the client over to the server to attach as a listener..
			serverProxy.addElementListener(clientServer);
			LOG.info("Client ready: "+clientDestination);
		} catch (JMSException e) {
			LOG.fatal(e);
		}

		setModel(guiModel);
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
