package org.omo.jjms;

import java.io.Serializable;
import java.rmi.server.UID;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJMSSession implements Session {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	protected final JJMSConnection connection;
	private final JJMSDestinationFactory queueFactory = new JJMSQueueFactory();
	private final JJMSDestinationFactory temporaryQueueFactory = new JJMSTemporaryQueueFactory();
	private final JJMSDestinationFactory topicFactory = new JJMSTopicFactory();
	
	private final boolean transacted;
	private final int acknowledgeMode;
	
	protected JJMSSession(JJMSConnection connection, boolean transacted, int acknolwedgeMode) {
		this.connection = connection;
		this.transacted = transacted;
		this.acknowledgeMode = acknolwedgeMode;
		logger.info("open");
	}
	
	protected void send(JJMSMessage message, JJMSDestination destination) throws JMSException {
		connection.send(message, destination);
	}

	// JMS
	
	@Override
	public void close() throws JMSException {
		logger.info("close");
	}

	@Override
	public void commit() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public QueueBrowser createBrowser(Queue arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return new JJMSMessageConsumer((JJMSDestination)destination, null);
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		return new JJMSMessageConsumer((JJMSDestination)destination, messageSelector);
	}

	@Override
	public MessageConsumer createConsumer(Destination arg0, String arg1,
			boolean arg2) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1)
			throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1,
			String arg2, boolean arg3) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Message createMessage() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		return new JJMSObjectMessage();
	}

	@Override
	public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
		return new JJMSObjectMessage(serializable);
	}

	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException {
		return new JJMSMessageProducer(this, destination);
	}

	@Override
	public Queue createQueue(String name) throws JMSException {
		return (Queue)connection.ensureDestination(name, queueFactory);
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return (TemporaryQueue)connection.ensureDestination(new UID().toString(), temporaryQueueFactory);
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		return new JJMSTextMessage();
	}

	@Override
	public TextMessage createTextMessage(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Topic createTopic(String name) throws JMSException {
		return (Topic)connection.ensureDestination(name, topicFactory);
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		return acknowledgeMode;
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean getTransacted() throws JMSException {
		return transacted;
	}

	@Override
	public void recover() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void rollback() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setMessageListener(MessageListener arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void unsubscribe(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

}