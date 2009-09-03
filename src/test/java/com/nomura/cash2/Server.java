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

public class Server extends FilterView<View> {

	private static final Log LOG = LogFactory.getLog(Server.class);

	interface Updater extends View<Updater> { };

	public Server(Query<View> query) throws JMSException {
		super(query);
	}

	@Override
	public void registerView(View view) {
		LOG.info("new Listener: " + view);
		upsert(view); // add listener to model...
		super.registerView(view);
	}

	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker1?persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueFactory queueFactory = new QueueFactory();
		int timeout = 5000;

		// arrange for the Listener component of our interface to become a server via JMS...
		RemotingFactory<View> factory = new RemotingFactory<View>(session, View.class, queueFactory, timeout);
		factory.createServer(new Server(new IdentityFilter<View>()));
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
