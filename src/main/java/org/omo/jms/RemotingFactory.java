package org.omo.jms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: reuse more code between these classes...
// TODO: support topic/multi-shot result aggregation
// TODO: server should probably support a lifecycle ?
// TODO: test multiple client/topic/multiple server scenarios...

public class RemotingFactory<T> {

	private final Session session;
	private final Class<?> interfaze;
	private final SimpleMethodMapper mapper;
	private final long timeout;
	private final MessageProducer producer;
	
	public RemotingFactory(Session session, Class<?> interfaze, long timeout) throws JMSException {
		this.session = session;
		this.interfaze = interfaze;
		this.mapper = new SimpleMethodMapper(interfaze);
		this.timeout = timeout;
		producer = session.createProducer(null);
	}
	
	//--------------------------------------------------------------------
	
	public class Server implements MessageListener {

		private final Logger logger;
		private final T target;
		private final MessageConsumer consumer;
		private final ExecutorService executorService; 

		public Server(T target, Destination destination, ExecutorService executorService) throws JMSException {
			this.target = target;
			this.executorService = executorService;
			logger = LoggerFactory.getLogger(Server.class);
			consumer = session.createConsumer(destination); // permanently allocates a thread... and an fd ? 
			logger.info("{}: consuming messages on: {}", System.identityHashCode(this), destination);
			consumer.setMessageListener(this);
		}
		
		@Override
		public void onMessage(final Message message) {
			//process(message);
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					process(message);
				}
			};
			executorService.execute(runnable);
		}
		
		public void process(Message message) {
			String correlationId = null;
			Destination replyTo = null;
			boolean isException = false;
			Object result = null;
			// input
			try {
				correlationId = message.getJMSCorrelationID();
				replyTo = message.getJMSReplyTo();
				ObjectMessage request = (ObjectMessage)message;
				AbstractClient.setCurrentSession(session);
				Invocation invocation = (Invocation)request.getObject();
				int methodIndex = invocation.getMethodIndex();
				Object args[] = invocation.getArgs();
				Method method = mapper.getMethod(methodIndex);
				logger.trace("RECEIVING: {} <- {}", method, message.getJMSDestination());
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

			if (isException)
				logger.warn("returning exception", (Throwable)result);
			if (correlationId != null && replyTo != null) {
				ObjectMessage response = null;
				try {
					response = session.createObjectMessage();
					response.setJMSCorrelationID(correlationId);
					Results results = new Results(isException, result);
					response.setObject(results);
					logger.trace("RESPONDING: {} -> {}",results, replyTo);
					producer.send(replyTo, response);
				} catch (JMSException e) {
				        logger.warn("problem replying to message: {}", response, e);
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------

	public T createServer(T target, Destination destination, ExecutorService executorService) throws JMSException {
		new Server(target, destination, executorService);
		return target;
	}
	
	public T createSynchronousClient(Destination destination, boolean trueAsync) throws IllegalArgumentException, JMSException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		return (T)Proxy.newProxyInstance(contextClassLoader, new Class[]{interfaze}, new SynchronousClient(session, destination, interfaze, timeout, true));
	}
	
	public T createSynchronousClient(String destinationName, boolean trueAsync) throws IllegalArgumentException, JMSException {
		return createSynchronousClient(session.createQueue(destinationName), trueAsync);
	}
	
	public AsynchronousClient createAsynchronousClient(Destination destination, boolean trueAsync) throws JMSException {
		return new AsynchronousClient(session, destination, interfaze, timeout, trueAsync);
	}
}
