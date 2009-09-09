package org.omo.core;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public class Main {

	public static void main(String[] args) throws JMSException {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
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
				names.add("Server.2.View");
				return names;
			}
		};

		// Server
		Server server = new Server(configuration);
		server.run();
		
		Client client = new Client(configuration);
		client.run();
	}

}
