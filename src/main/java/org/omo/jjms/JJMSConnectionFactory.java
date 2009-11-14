package org.omo.jjms;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentTreeMap;

public class JJMSConnectionFactory implements ConnectionFactory {

	private class Job implements Runnable {

		private final JJMSMessage message;
		private final JJMSDestination destination;
		
		private Job(JJMSMessage message, JJMSDestination destination) {
			this.message = message;
			this.destination = destination;
		}

		@Override
		public void run() {
			try {
				destination.dispatch(message);
			} finally {
				lock.unlock(); // decrement lock count - this message is now being processed...
			}
		}
		
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
	private final ExecutorService threadPool;
	private final Lock lock;

	private volatile IPersistentMap nameToDestination= PersistentTreeMap.EMPTY;
	private volatile boolean running;
	private volatile Thread thread;
	
	public JJMSConnectionFactory(ExecutorService threadPool, Lock lock) {
		logger.debug("create");
		this.threadPool = threadPool;
		this.lock = lock;
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
		lock.lock(); // increment lock count - this message is now in-flight...
		jobs.add(new Job(message, destination));
	}

	
	public void start() {
		logger.debug("start");
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				while (running) {
					try {
						threadPool.execute(jobs.take());
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}});
		thread.start();
	}

	public void stop() {
		running = false;
		logger.debug("stop");
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
