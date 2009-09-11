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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AsynchronousClient extends Client {
	
	private final Log log = LogFactory.getLog(getClass());
	private final Map<String, AsyncInvocationListener> correlationIdToListener = new ConcurrentHashMap<String, AsyncInvocationListener>();

	AsynchronousClient(Session session, Destination invocationDestination, Class<?> interfaze, long timeout) throws JMSException {
		super(session, invocationDestination, interfaze, timeout);
	}

	public void invoke(Method method, Object[] args, AsyncInvocationListener listener) throws JMSException {
		ObjectMessage message = session.createObjectMessage();
		Integer methodIndex = mapper.getKey(method);
		message.setObject(new Invocation(methodIndex, args));
		String correlationId = "" + count++;
		message.setJMSCorrelationID(correlationId);
		message.setJMSReplyTo(resultsQueue);

		correlationIdToListener.put(correlationId, listener); // TODO: support a timeout after which this listener is removed...
		log.trace("SENDING: " + message + " -> " + invocationDestination);
		producer.send(invocationDestination, message);			
	}

	@Override
	public void onMessage(Message message) {
		try{log.info("RECEIVING: " + message + " <- " + message.getJMSDestination());}catch(Exception e){};
		try {
			String correlationID = message.getJMSCorrelationID();
			AsyncInvocationListener listener = correlationIdToListener.remove(correlationID); // one-shot - parameterize 
			if (listener == null) {
				log.warn("no listener for message: " + message);
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
			log.error("problem extracting data from message; "+message);
		}
	}
}
