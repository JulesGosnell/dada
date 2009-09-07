/**
 * 
 */
package org.omo.jms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SynchronousClient extends Client implements InvocationHandler, Serializable {

	private final Log log = LogFactory.getLog(getClass());
	private /* final */ transient Map<String, Exchanger<Results>> correlationIdToResults;
	
	public SynchronousClient(Session session, Destination invocationDestination, Class<?> interfaze, long timeout) throws JMSException {
		super(session, invocationDestination, interfaze, timeout);
		correlationIdToResults = new ConcurrentHashMap<String, Exchanger<Results>>();
	}
	
	//@Override
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		correlationIdToResults = new ConcurrentHashMap<String, Exchanger<Results>>();
		ois.defaultReadObject();
	}

	public void onMessage(Message message) {
		try {
			String correlationID = message.getJMSCorrelationID();
			Exchanger<Results> exchanger = correlationIdToResults.remove(correlationID);
			if (exchanger == null) {
				log.warn("no exchanger for message: " + message);
			} else {
				ObjectMessage response = (ObjectMessage)message;
				Results results = (Results)response.getObject();
				log.info("RECEIVING: " + results + " <- " + message.getJMSDestination());
				exchanger.exchange(results, timeout, TimeUnit.MILLISECONDS);
			}
		} catch(JMSException e) {
			log.warn("problem unpacking message: " + message);
		} catch (InterruptedException e) {
			// TODO: how should we deal with this...
		} catch (TimeoutException e) {
			log.warn("timed out waiting for exchange: " + message);
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		ObjectMessage message = session.createObjectMessage();
		Integer methodIndex = mapper.getKey(method);
		if (methodIndex == null) {
			// log.warn("unproxied method invoked: " + method);
			return method.invoke(this, args);
		}
		message.setObject(new Invocation(methodIndex, args));
		String correlationId = "" + count++;
		message.setJMSCorrelationID(correlationId);
		message.setJMSReplyTo(resultsQueue);
		Exchanger<Results> exchanger = new Exchanger<Results>();
		correlationIdToResults.put(correlationId, exchanger);
		log.info("SENDING: " + method + " -> " + invocationDestination);
		producer.send(invocationDestination, message);
		try {
			Results results = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
			Object value = results.getValue();
			if (results.isException())
				throw (Exception)value;
			else
				return value;
		} catch (TimeoutException e) {
			correlationIdToResults.remove(correlationId);
			log.warn("timedout waiting for results from invocation: " + method);
			return null;
		}
		
	}
	
	public String toString() {
		return "<"+getClass().getSimpleName()+": "+invocationDestination+">";
	}
	
}