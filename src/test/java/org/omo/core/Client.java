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

	protected final JView all = new JView();
	protected final JView partition0 = new JView();
	protected final JView partition1 = new JView();
	protected final JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, partition0, partition1);
	protected final JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, all, splitPane2);
	protected final JPanel panel = new JPanel();
	protected final LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
	protected final JFrame frame = new JFrame("Demo");

	private Session session;
	private int timeout;
	
	public void setSession(Session session) {
		this.session = session;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void run() {
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
			partition0.setModel(guiModel1);
			
			// pass the client over to the server to attach as a listener..
			Collection<Trade> trades = serverProxy.registerView(clientServer);
			guiModel1.upsert(trades);
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
			partition1.setModel(guiModel2);

			// pass the client over to the server to attach as a listener..
			Collection<Trade> trades = serverProxy.registerView(clientServer);
			guiModel2.upsert(trades);

			LOG.info("Client ready: "+clientDestination);
		} catch (JMSException e) {
			LOG.fatal(e);
		}

		panel.add(splitPane2);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		SwingUtilities.invokeLater(this);
	}
	
	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		int timeout = 60000;

		Client client = new Client();
		client.setSession(session);
		client.setTimeout(timeout);
		client.run();
	}

}
