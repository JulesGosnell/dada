package com.nomura.consensus.jms;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO: reuse more code between these classes...
// TODO: support topic/multi-shot result aggregation
// TODO: server should probably support a lifecycle ?
// TODO: test multiple client/topic/multiple server scenarios...

public class RemotingFactory<T> {

	private final Session session;
	private final Class<?> interfaze;
	private final Method[] indexToMethod;
	private final Map<Method, Integer> methodToIndex = new HashMap<Method, Integer>();
	private final Destination invocationDestination;
	private final long timeout;
	
	public RemotingFactory(Session session, Class<?> interfaze, DestinationFactory factory, long timeout) throws JMSException {
		this.session = session;
		this.interfaze = interfaze;
		this.indexToMethod = this.interfaze.getMethods();
		int index = 0;
		for (Method method : this.indexToMethod)
			methodToIndex.put(method, index++);
		this.invocationDestination = factory.create(session, this.interfaze.getCanonicalName());
		this.timeout = timeout;
	}
	
	//--------------------------------------------------------------------
	
	public class Server implements MessageListener {

		private final Log log = LogFactory.getLog(Server.class);

		private final T target;
		private final MessageProducer producer;
		private final MessageConsumer consumer;

		public Server(T target) throws JMSException {
			this.target = target;
			this.producer = session.createProducer(null);
			this.consumer = session.createConsumer(invocationDestination);
			this.consumer.setMessageListener(this);
		}

		@Override
		public void onMessage(Message message) {
			String correlationId = null;
			Destination replyTo = null;
			boolean isException = false;
			Object result = null;
			// input
			try {
				correlationId = message.getJMSCorrelationID();
				replyTo = message.getJMSReplyTo();
				ObjectMessage request = (ObjectMessage)message;
				Invocation invocation = (Invocation)request.getObject();
				int methodIndex = invocation.getMethodIndex();
				Object args[] = invocation.getArgs();
				Method method = indexToMethod[methodIndex];
				result = method.invoke(Server.this.target, args);
			} catch (JMSException e) {
				isException = true;
				result = e;
			} catch (IllegalAccessException e) {
				isException = true;
				result = e;
			} catch (InvocationTargetException e) {
				isException = true;
				result = e.getTargetException();
			}

			if (correlationId != null && replyTo != null) {
				ObjectMessage response = null;
				try {
					response = session.createObjectMessage();
					response.setJMSCorrelationID(correlationId);
					response.setObject(new Results(isException, result));
					producer.send(replyTo, response);
				} catch (JMSException e) {
					log.warn("problem replying to message: " + response, e);
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------
	
	public T createServer(T target) throws JMSException {
		new Server(target);
		return target;
	}
	
	//-----------------------------------------------------------------------------
	
	public abstract class Client implements MessageListener {

		protected final UUID uuid = UUID.randomUUID();
		protected final MessageProducer producer;
		protected final Queue resultsQueue;
		protected final MessageConsumer consumer;
		
		protected int count;
		
		public Client() throws JMSException {
			this.producer = session.createProducer(invocationDestination);
			this.resultsQueue = session.createQueue(interfaze.getCanonicalName() + "." + uuid);
			this.consumer = session.createConsumer(resultsQueue);
			this.consumer.setMessageListener(this);
		}

		@Override
		public abstract void onMessage(Message message);
	}

	//-----------------------------------------------------------------------------

	public class SynchronousClient extends Client implements InvocationHandler {

		private final Log log = LogFactory.getLog(SynchronousClient.class);
		private final Map<String, Exchanger<Results>> correlationIdToResults = new ConcurrentHashMap<String, Exchanger<Results>>();
		
		public SynchronousClient() throws JMSException {
			super();
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
			Integer methodIndex = methodToIndex.get(method);
			message.setObject(new Invocation(methodIndex, args));
			String correlationId = "" + count++;
			message.setJMSCorrelationID(correlationId);
			message.setJMSReplyTo(resultsQueue);
			Exchanger<Results> exchanger = new Exchanger<Results>();
			correlationIdToResults.put(correlationId, exchanger);
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
	}

	//-------------------------------------------------------------------------------------
	
	public T createSynchronousClient() throws IllegalArgumentException, JMSException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		return (T)Proxy.newProxyInstance(contextClassLoader, new Class[]{interfaze}, new SynchronousClient());
	}
	
	public class AsynchronousClient extends Client {
		
		private final Log log = LogFactory.getLog(AsynchronousClient.class);
		private final Map<String, AsyncInvocationListener> correlationIdToListener = new ConcurrentHashMap<String, AsyncInvocationListener>();

		private AsynchronousClient() throws JMSException {
			super();
		}

		public void invoke(Method method, Object[] args, AsyncInvocationListener listener) throws JMSException {
			ObjectMessage message = session.createObjectMessage();
			Integer methodIndex = methodToIndex.get(method);
			message.setObject(new Invocation(methodIndex, args));
			String correlationId = "" + count++;
			message.setJMSCorrelationID(correlationId);
			message.setJMSReplyTo(resultsQueue);

			correlationIdToListener.put(correlationId, listener); // TODO: support a timeout after which this listener is removed...
			producer.send(invocationDestination, message);			
		}

		@Override
		public void onMessage(Message message) {
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
	
	public AsynchronousClient createAsynchronousClient() throws JMSException {
		return new AsynchronousClient();
	}
}
