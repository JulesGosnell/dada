package com.nomura.cash2;

import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.SwingUtilities;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nomura.consensus.jms.QueueFactory;
import com.nomura.consensus.jms.RemotingFactory;

public class Main {

	private static final Log LOG = LogFactory.getLog(Server.class);

	public static void main(String[] args) throws JMSException {
		int timeout = 5000;
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		// Server
		{
			Connection connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue("Server");
 
			RemotingFactory<Listener> serverFactory = new RemotingFactory<Listener>(session, Listener.class, destination, timeout);
			serverFactory.createServer(new Server(new IdentityFilter<Listener>()));
			LOG.info("Server ready...");
		}
		// Client
		{
			Connection connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// need an object that is a Listener and a TableModel - then

			// create a client-side proxy for the Server
			Destination serverDestination = session.createQueue("Server");
			RemotingFactory<View> clientFactory = new RemotingFactory<View>(session, View.class, serverDestination, timeout);
			View serverProxy = clientFactory.createSynchronousClient();

			// create a Client
			Listener client = new TestListener();
			
			// create a client-side server to support callbacks on client
			Destination clientDestination = session.createQueue("Client");
			RemotingFactory<Listener> serverFactory = new RemotingFactory<Listener>(session, Listener.class, clientDestination, timeout);
			Listener clientServer = serverFactory.createServer(client);

			// pass the client over to the server to attach as a listener..
			serverProxy.addElementListener(clientServer);
			LOG.info("Client ready...");
		}

		// TODO: need to hook ClientListener to Swing Client
		SwingUtilities.invokeLater(new Client());

		// keep going...
		while (true)
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

}
