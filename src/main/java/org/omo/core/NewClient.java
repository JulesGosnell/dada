package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.TableModelView.Mapper;
import org.omo.jms.RemotingFactory;
import org.omo.old.demo.JView;

public class NewClient {
	
	private final static Log LOG = LogFactory.getLog(NewClient.class);

	private final Mapper<String, String> mapper = new Mapper<String, String>() {

		@Override
		public Object getField(String value, int index) {
			switch (index) {
			case 0: return value;
			}
			throw new IllegalArgumentException("index out of bounds: " + index);
		}

		@Override
		public List<String> getFieldNames() {
			List<String> names = new ArrayList<String>();
			names.add("name");
			return names; // TODO: avoid reallocation...
		}

		@Override
		public String getKey(String value) {
			return value;
		}
	};
	
	public NewClient(String serverName, ConnectionFactory connectionFactory, int timeout) throws JMSException {
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		TableModelView<String, String> guiModel = new TableModelView<String, String>(mapper);

		// create a client-side proxy for the Server
		Destination serverDestination = session.createQueue(serverName + "." + "MetaModel");
		RemotingFactory<Model<String, String>> clientFactory = new RemotingFactory<Model<String, String>>(session, Model.class, serverDestination, timeout);
		Model<String, String> serverProxy = clientFactory.createSynchronousClient();

		// create a Client

		// create a client-side server to support callbacks on client
		Destination clientDestination = session.createQueue("Client.all.Listener");
		RemotingFactory<View<String, String>> serverFactory = new RemotingFactory<View<String, String>>(session, View.class, clientDestination, timeout);
		serverFactory.createServer(guiModel);
		View<String, String> clientServer = serverFactory.createSynchronousClient();

		// pass the client over to the server to attach as a listener..
		Collection<String> models = serverProxy.registerView(clientServer);
		guiModel.upsert(models);
		LOG.info("Client ready: "+clientDestination);

		JView jview = new JView(guiModel);
		JFrame frame = new JFrame("Client");
		JPanel panel = new JPanel();
		panel.add(jview);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		final String serverName = (args.length == 0 ? "Server" : args[0]);
		String url = "peer://" + serverName + "/broker0?broker.persistent=false&broker.useJmx=false";
		LOG.info("Broker URL: " +url);
		final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
		SwingUtilities.invokeAndWait(new Runnable() {
			
			@Override
			public void run() {
				try {
					new NewClient(serverName, connectionFactory, 60000);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		// tidy up...
	}

}
