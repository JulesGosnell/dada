package com.nomura.cash2;

import java.util.ArrayList;
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

import com.nomura.consensus.jms.RemotingFactory;

public class Main {

	private static final Log LOG = LogFactory.getLog(Main.class);

	public static void main(String[] args) throws JMSException {
		int timeout = 60000;
		//ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?broker.persistent=false&broker.useJmx=false");
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// Server
		{
			Model<Trade> server = new TradeGenerator(10,100L);
			List<TradePartition> tradePartitions = new ArrayList<TradePartition>();
			TradePartition partition0 = new TradePartition(0);
			tradePartitions.add(partition0);
			TradePartition partition1 = new TradePartition(1);
			tradePartitions.add(partition1);
			TradePartitioner partitioner = new TradePartitioner(tradePartitions);
			server.registerView(partitioner);
			{
				Destination destination = session.createQueue("Server.0.View");
				RemotingFactory<Model<Trade>> serverFactory = new RemotingFactory<Model<Trade>>(session, Model.class, destination, timeout);
				serverFactory.createServer(partition0);
			}
			{
				Destination destination = session.createQueue("Server.1.View");
				RemotingFactory<Model<Trade>> serverFactory = new RemotingFactory<Model<Trade>>(session, Model.class, destination, timeout);
				serverFactory.createServer(partition1);
			}
			server.start();
			LOG.info("Server ready: Server.<partition>.View");
		}

		{
			Client client = new Client();
			client.setTimeout(timeout);
			client.setSession(session);
			SwingUtilities.invokeLater(client);
		}
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		{
//			Client client = new Client();
//			client.setTimeout(timeout);
//			client.setSession(session);
//			SwingUtilities.invokeLater(client);
//		}
		
		// keep going...
		while (true)
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

}
