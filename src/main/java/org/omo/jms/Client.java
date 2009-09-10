/**
 * 
 */
package org.omo.jms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class Client implements MessageListener, Serializable {

	protected long timeout;
	protected Class<?> interfaze;
	protected Destination invocationDestination;

	protected transient Log log;
	protected transient UUID uuid;
	protected transient Session session;
	protected transient MessageProducer producer;
	protected transient Queue resultsQueue;
	protected transient MessageConsumer consumer;
	protected /* final */ SimpleMethodMapper mapper;
	protected transient int count; // used by subclasses
	
	public Client(Session session, Destination invocationDestination, Class<?> interfaze, long timeout) throws JMSException {
		init(session, invocationDestination, interfaze, timeout);
	}
	
	protected void init(Session session, Destination invocationDestination, Class<?> interfaze, long timeout) throws JMSException {
		log = LogFactory.getLog(getClass());
		this.interfaze = interfaze;
		mapper = new SimpleMethodMapper(interfaze);
		this.invocationDestination = invocationDestination;
		this.timeout = timeout;
		this.uuid = UUID.randomUUID();

		this.session = session;
		this.producer = session.createProducer(invocationDestination);
		this.resultsQueue = session.createQueue(interfaze.getCanonicalName() + "." + uuid);
		this.consumer = session.createConsumer(resultsQueue);
		this.consumer.setMessageListener(this);
	}

	//@Override
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(invocationDestination);
		oos.writeObject(interfaze);
		oos.writeLong(timeout);
	}
	
	//@Override
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		Session session = getCurrentSession();
		Destination invocationDestination = (Destination)ois.readObject();
		Class<?> interfaze= (Class<?>)ois.readObject();
		long timeout = ois.readLong();
		try {
			init(session, invocationDestination, interfaze, timeout);
		} catch (JMSException e) {
			log.error("unexpected problem reconstructing client proxy", e);
		}
	}
	
	protected final static ThreadLocal<Session> currentSession = new ThreadLocal<Session>(); // TODO: encapsulate

	public static void setCurrentSession(Session session) {
		currentSession.set(session);
	}

	public static Session getCurrentSession() {
		return currentSession.get();
	}
	

}
