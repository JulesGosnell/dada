package org.omo.jjms;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJMSConnection implements Connection {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final JJMSConnectionFactory connectionFactory;
	
	private volatile boolean running;
	private volatile ExceptionListener exceptionListener;

	protected JJMSConnection(JJMSConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		logger.info("open");
	}
	
	protected JJMSDestination ensureDestination(String name, JJMSDestinationFactory destinationFactory) {
		return connectionFactory.ensureDestination(name, destinationFactory);
	}

	public void send(JJMSMessage message, JJMSDestination destination) throws JMSException {
		connectionFactory.send(message, destination);
	}

	// JMS...
	
	@Override
	public void close() throws JMSException {
		logger.info("close");
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return new JJMSSession(this, transacted, acknowledgeMode);
	}

	@Override
	public String getClientID() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setClientID(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
		exceptionListener = listener;
	}

	@Override
	public void start() throws JMSException {
		running = true;
	}

	@Override
	public void stop() throws JMSException {
		running = false;
	}

}
