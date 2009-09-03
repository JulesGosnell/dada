package com.nomura.cash2;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nomura.consensus.jms.QueueFactory;
import com.nomura.consensus.jms.RemotingFactory;

public class Server extends FilterView<Listener> {

	private static final Log LOG = LogFactory.getLog(Server.class);

	interface Updater extends Listener<Updater> { };

	public Server(Query<Listener> query) throws JMSException {
		super(query);
	}

	@Override
	public void addElementListener(Listener listener) {
		LOG.info("new Listener: " + listener);
		upsert(listener); // add listener to model...
		super.addElementListener(listener);
	}

	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker1?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueFactory queueFactory = new QueueFactory();
		int timeout = 5000;

		// arrange for the Listener component of our interface to become a server via JMS...
		RemotingFactory<Listener> factory = new RemotingFactory<Listener>(session, Listener.class, queueFactory, timeout);
		factory.createServer(new Server(new IdentityFilter<Listener>()));
		LOG.info("ready...");

		// keep going...
		while (true)
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
}
