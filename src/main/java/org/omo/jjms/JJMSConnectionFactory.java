package org.omo.jjms;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class JJMSConnectionFactory implements ConnectionFactory {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private volatile IPersistentMap nameToDestination= PersistentTreeMap.EMPTY;
	private final BlockingQueue<Job> jobs = new ArrayBlockingQueue<Job>(1000);
	private final ExecutorService threadPool = Executors.newFixedThreadPool(20);

	public JJMSConnectionFactory() {
		logger.debug("create");
	}
	
	protected JJMSDestination ensureDestination(String name, JJMSDestinationFactory destinationFactory) {
		JJMSDestination destination = (JJMSDestination) nameToDestination.valAt(name);
		if (destination == null) {
			nameToDestination = nameToDestination.assoc(name, destination = destinationFactory.create(name));
		}
		return destination;
	}

	public void send(JJMSMessage message, JJMSDestination destination) throws JMSException {
		message.setJMSDestination(destination);
		jobs.add(new Job(message, destination));
	}

	public void run() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
					threadPool.execute(jobs.take());
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}}).start();
	}
	
	// JMS
	
	@Override
	public Connection createConnection() throws JMSException {
		return new JJMSConnection(this);
	}

	@Override
	public Connection createConnection(String arg0, String arg1) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

}
