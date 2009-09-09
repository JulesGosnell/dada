package org.omo.core;

import java.awt.LayoutManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.jms.RemotingFactory;
import org.omo.old.demo.JView;


public class Client implements Runnable {

	private static final Log LOG = LogFactory.getLog(Client.class);

	protected JView universal;
	protected List<JView> partitions = new ArrayList<JView>();
	protected JView aggregate;
	protected JPanel panel;
	protected LayoutManager layout;
	protected final JFrame frame = new JFrame("Demo");

	private Configuration configuration;

	public Client(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public void run() {
		// Client
		TradeTableModel guiModel0 = new TradeTableModel();
		Session session = configuration.getSession();
		int timeout = configuration.getTimeout();
		
		try	{
			// create a client-side proxy for the Server
			Destination serverDestination = session.createQueue(configuration.getUniversalModelName());
			RemotingFactory<Model<Integer, Trade>> clientFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, serverDestination, timeout);
			Model<Integer, Trade> serverProxy = clientFactory.createSynchronousClient();

			// create a Client

			// create a client-side server to support callbacks on client
			Destination clientDestination = session.createQueue("Client.all.Listener");
			RemotingFactory<View<Integer, Trade>> serverFactory = new RemotingFactory<View<Integer, Trade>>(session, View.class, clientDestination, timeout);
			serverFactory.createServer(guiModel0);
			View<Integer, Trade> clientServer = serverFactory.createSynchronousClient();
			
			// pass the client over to the server to attach as a listener..
			Collection<Trade> trades = serverProxy.registerView(clientServer);
			guiModel0.upsert(trades);
			LOG.info("Client ready: "+clientDestination);
		} catch (JMSException e) {
			LOG.fatal(e);
		}
		universal = new JView(guiModel0);
		
		int i = 0;
		for (String name : configuration.getPartitionModelNames()) {
			TradeTableModel guiModel = new TradeTableModel();
			try	{
				// create a client-side proxy for the Server
				Destination serverDestination = session.createQueue(name);
				RemotingFactory<Model<Integer, Trade>> clientFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, serverDestination, timeout);
				Model<Integer, Trade> serverProxy = clientFactory.createSynchronousClient();

				// create a Client

				// create a client-side server to support callbacks on client
				Destination clientDestination = session.createQueue("Client."+i+".Listener");
				RemotingFactory<View<Integer, Trade>> serverFactory = new RemotingFactory<View<Integer, Trade>>(session, View.class, clientDestination, timeout);
				serverFactory.createServer(guiModel);
				View<Integer, Trade> clientServer = serverFactory.createSynchronousClient();

				// pass the client over to the server to attach as a listener..
				Collection<Trade> trades = serverProxy.registerView(clientServer);
				guiModel.upsert(trades);
				partitions.add(new JView(guiModel));
				LOG.info("Client ready: "+clientDestination);
			} catch (JMSException e) {
				LOG.fatal(e);
			}
		}

		Iterator<JView> it = partitions.iterator();
		JComponent top;
		JComponent bot;
		top = it.next();
		while (it.hasNext()) {
			bot = it.next();
			top = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bot);
		}

		aggregate = new JView(guiModel0);

		JComponent left = universal;
		JComponent right = top;
		left = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		right = aggregate; 
		left = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		
		panel = new JPanel();
		layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.add(left);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Configuration configuration = new Configuration() {
			
			@Override
			public String getUniversalModelName() {
				return "Server.all.View";
			}
			
			@Override
			public int getTimeout() {
				return 60000;
			}
			
			@Override
			public Session getSession() {
				return session;
			}
			
			@Override
			public List<String> getPartitionModelNames() {
				List<String> names = new ArrayList<String>();
				names.add("Server.0.View");
				names.add("Server.1.View");
				return names;
			}
		};
		
		Client client = new Client(configuration);
		client.run();

		// tidy up..
	}

}
