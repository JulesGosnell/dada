package org.omo.jms;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemotingTestCase extends TestCase {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final ExecutorService executorservice = Executors.newFixedThreadPool(10);
	
	protected ConnectionFactory connectionFactory;
	protected Connection connection;
	protected Session session;
	protected int timeout;
	protected RemotingFactory<Peer> remotingFactory;
	protected Queue serverQueue;
	protected Peer serverImpl;
	protected Peer serverProxy; // proxy to the server...

	protected AsynchronousClient asyncServerProxy;
	
	protected abstract ConnectionFactory getConnnectionFactory();
	
	@Override
	protected void setUp() throws Exception {
		connectionFactory = getConnnectionFactory(); 
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		timeout = 5000;

		remotingFactory = new RemotingFactory<Peer>(session, Peer.class, timeout);
		serverQueue = session.createQueue(Peer.class.getCanonicalName());
		serverImpl = remotingFactory.createServer(new PeerImpl(), serverQueue, executorservice);
		serverProxy = remotingFactory.createSynchronousClient(serverQueue, true);
		// hmmm... - we are not using this at the moment - but we'll hang onto it...
		asyncServerProxy = remotingFactory.createAsynchronousClient(serverQueue, true);
	}

	@Override
	protected void tearDown() throws Exception {
		asyncServerProxy = null;
		serverProxy = null;
		serverImpl = null;
		serverQueue = null;
		remotingFactory = null;
		
		timeout = 0;
		session.close();
		session = null;
		connection.stop();
		connection.close();
		connection = null;
		connectionFactory = null;
	}
	
	public void testSynchronousInvocation() throws Exception {
		String string = "test";
		assertEquals(serverImpl.hashcode(string), serverProxy.hashcode(string));
	}

	public void testSynchronousInvocationAsync() throws Exception {
		final String string = "test";
		final CountDownLatch latch = new CountDownLatch(1);
		Method method = Peer.class.getMethod("hashcode", new Class[]{String.class});
		asyncServerProxy.invoke(method, new Object[]{string}, new AsyncInvocationListener(){

			@Override
			public void onError(Exception exception) {
				assertTrue(false);
			}

			@Override
			public void onResult(Object value) {
				assertEquals(serverImpl.hashcode(string), value);
				latch.countDown();
			}});
		latch.await(5, TimeUnit.SECONDS);

	}

	public void testSynchronousInvocationThrowingException() throws Exception {
		try {
			serverProxy.throwException();
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof PeerException);
		}
	}

	public void testSynchronousInvocationThrowingExceptionAsyn() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Method method = Peer.class.getMethod("throwException", (Class<?>[])null);
		asyncServerProxy.invoke(method, null, new AsyncInvocationListener(){

			@Override
			public void onError(Exception exception) {
				assertTrue(exception instanceof PeerException);
				latch.countDown();
			}

			@Override
			public void onResult(Object value) {
				assertTrue(false);
			}});
		latch.await(5, TimeUnit.SECONDS);
	}

	public void testSynchronousInvocationReturningException() throws Exception {
		assertTrue(serverProxy.returnException() instanceof PeerException);
	}

	public void testSynchronousInvocationReturningExceptionAsync() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Method method = Peer.class.getMethod("returnException", (Class<?>[])null);
		asyncServerProxy.invoke(method, null, new AsyncInvocationListener(){

			@Override
			public void onError(Exception exception) {
				assertTrue(false);
			}

			@Override
			public void onResult(Object value) {
				assertTrue(value instanceof PeerException);
				latch.countDown();
			}});
		latch.await(5, TimeUnit.SECONDS);
	}

	public void testReentrantSynchronousInvocations() throws Exception {
		Queue clientQueue = session.createTemporaryQueue();
		final Peer clientImpl = remotingFactory.createServer(new PeerImpl(), clientQueue, executorservice);
		final Peer clientProxy = remotingFactory.createSynchronousClient(clientQueue, true);

		// call server passing client and data.
		// server calls client passing data
		// client returns data to server
		// server returns data to us...
		Object data = "data";
		assertTrue(data.equals(serverProxy.register(clientProxy, data)));
	}

	
	public void testSendAsyncReceive(Serializable serializable) throws Exception {
		TemporaryQueue queue = session.createTemporaryQueue();
		MessageProducer producer = session.createProducer(queue);
		MessageConsumer consumer = session.createConsumer(queue);
		final CountDownLatch latch = new CountDownLatch(1);
		consumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message arg0) {
				try {
					logger.info(((ObjectMessage)arg0).getObject().toString());
				} catch (JMSException e) {
					// ignore
				}
				latch.countDown();
			}
		});
		ObjectMessage message = session.createObjectMessage();
		message.setObject(serializable);
		producer.send(message);
		assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
	}
	
	public void testSendAsyncReceiveSerialisable() throws Exception {
		testSendAsyncReceive("hello");
	}

}

