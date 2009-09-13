package org.omo.core;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Server implements Runnable {

	private static final Log LOG = LogFactory.getLog(Server.class);

	protected final Configuration configuration;

	public Server(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void run() {
//		try {
//			Session session = configuration.getSession();
//			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, configuration.getTimeout());
//
//			Model<Integer, Trade> universal = new TradeFeed("TradeFeed", 10,100L);
//			serverFactory.createServer(universal, session.createQueue(configuration.getUniversalModelName()));
//			universal.start();
//
//			List<TradePartition> tradePartitions = new ArrayList<TradePartition>();
//			int i = 0;
//			for (String name : configuration.getPartitionModelNames()) {
//				TradePartition partition = new TradePartition("TradePartition."+i, i++);
//				tradePartitions.add(partition);
//				serverFactory.createServer(partition, session.createQueue(name));
//				partition.start();
//			}
//
//			TradePartitioner partitioner = new TradePartitioner(tradePartitions);
//			// partitioner.start();
//
//			Collection<Trade> trades = universal.registerView(partitioner);
//			partitioner.upsert(trades);
//			LOG.info("Models ready");
//		} catch (JMSException e) {
//			LOG.fatal("something went wrong");
//		}
	}

	public static void main(String[] args) throws JMSException {
		int timeout = 60000;
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://groupa/broker2?broker.persistent=false&broker.useJmx=false");
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
		
		Server server = new Server(configuration);
		server.run();

		// keep going...
		while (true)
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
}
