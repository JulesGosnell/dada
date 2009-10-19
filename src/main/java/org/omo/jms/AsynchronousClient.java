/**
 * 
 */
package org.omo.jms;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousClient extends AbstractClient {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, AsyncInvocationListener> correlationIdToListener = new ConcurrentHashMap<String, AsyncInvocationListener>();

	public AsynchronousClient(Session session, Destination destination, Class<?> interfaze, long timeout, boolean trueAsync) throws JMSException {
		super(session, destination, interfaze, timeout, trueAsync);
	}

	public void invoke(Method method, Object[] args, AsyncInvocationListener listener) throws JMSException {
		ObjectMessage message = session.createObjectMessage();
		Integer methodIndex = mapper.getKey(method);
		message.setObject(new Invocation(methodIndex, args));
		String correlationId = "" + count++;
		message.setJMSCorrelationID(correlationId);
		message.setJMSReplyTo(resultsQueue);

		correlationIdToListener.put(correlationId, listener); // TODO: support a timeout after which this listener is removed...
		logger.trace("SENDING: " + message + " -> " + destination);
		producer.send(destination, message);			
	}

	@Override
	public void onMessage(Message message) {
		try{logger.info("RECEIVING: " + message + " <- " + message.getJMSDestination());}catch(Exception e){};
		try {
			String correlationID = message.getJMSCorrelationID();
			AsyncInvocationListener listener = correlationIdToListener.remove(correlationID); // one-shot - parameterize 
			if (listener == null) {
				logger.warn("no listener for message: " + message);
			} else {
				ObjectMessage response = (ObjectMessage)message;
				Results results = (Results)response.getObject();
				Object value = results.getValue();
				if (results.isException()) {
					listener.onError((Exception)value);
				} else {
					listener.onResult(value);
				}
			}
		} catch (JMSException e) {
			logger.error("problem extracting data from message; "+message);
		}
	}
}
