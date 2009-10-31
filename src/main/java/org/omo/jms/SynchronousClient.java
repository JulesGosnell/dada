/**
 * 
 */
package org.omo.jms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronousClient extends AbstractClient implements InvocationHandler, Serializable {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private /* final */ transient Map<String, Exchanger<Results>> correlationIdToResults;
	
	public SynchronousClient(Session session, Destination destination, Class<?> interfaze, long timeout, boolean trueAsync) throws JMSException {
		super(session, destination, interfaze, timeout, trueAsync);
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
			       logger.warn("no exchanger for message: {}", message);
			} else {
				ObjectMessage response = (ObjectMessage)message;
				Results results = (Results)response.getObject();
				logger.trace("RECEIVING: {} <- {}", results, message.getJMSDestination());
				exchanger.exchange(results, timeout, TimeUnit.MILLISECONDS);
			}
		} catch(JMSException e) {
		        logger.warn("problem unpacking message: ", message);
		} catch (InterruptedException e) {
			// TODO: how should we deal with this...
		} catch (TimeoutException e) {
		        logger.warn("timed out waiting for exchange: {}", message);
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		ObjectMessage message = session.createObjectMessage();
		Integer methodIndex = mapper.getKey(method);
		if (methodIndex == null) {
			// log.warn("unproxied method invoked: {}", method);
			return method.invoke(this, args);
		}
		message.setObject(new Invocation(methodIndex, args));
		
		// TODO: whether a method is to be used asynchronously should be stored with it to save runtime overhead...
		boolean async = trueAsync && method.getReturnType().equals(Void.TYPE) && method.getExceptionTypes().length == 0;
		
		if (async) {
		        logger.trace("SENDING ASYNC: {} -> {}", method, destination);
			producer.send(destination, message);
			return null;
		} else {
			String correlationId = "" + count++;
			message.setJMSCorrelationID(correlationId);
			message.setJMSReplyTo(resultsQueue);
			Exchanger<Results> exchanger = new Exchanger<Results>();
			correlationIdToResults.put(correlationId, exchanger);
			logger.trace("SENDING SYNC: {} -> {}", method, destination);
			producer.send(destination, message);
			long start = System.currentTimeMillis();
			try {
				Results results = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
				Object value = results.getValue();
				if (results.isException())
					throw (Exception)value;
				else
					return value;
			} catch (TimeoutException e) {
				long elapsed = System.currentTimeMillis() - start;
				correlationIdToResults.remove(correlationId);
				logger.warn("timeout was: {}", timeout);
				logger.warn("timed out, after " + elapsed + " millis, waiting for results from invocation: " + method + " on " + destination); // TODO: SLF4j-ise
				throw e;
			}
		}
		
	}
	
	public String toString() {
		return "<"+getClass().getSimpleName()+": "+destination+">";
	}
	
	public boolean equals(Object object) {
		// strip off proxy if necessary
		Object that = Proxy.isProxyClass(object.getClass())?Proxy.getInvocationHandler(object):object;
		return (that instanceof SynchronousClient && this.destination.equals(((SynchronousClient)that).destination));
	}
}
