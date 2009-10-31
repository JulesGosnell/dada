package org.omo.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import junit.framework.TestCase;

//import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.omo.jjms.JJMSConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelTestCase extends TestCase {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	Munger server;
	Munger client;
	String string;
	String mungedString;
	JndiContext jndiContext;
	CamelContext camelContext;
	ConnectionFactory connectionFactory;

	protected void setUp() throws Exception {
		super.setUp();
		server = new MungerImpl();
		string = "hello";
		mungedString = server.munge(string);
		jndiContext = new JndiContext();
		jndiContext.bind("munger", server);
		camelContext = new DefaultCamelContext(jndiContext);
//		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useShutdownHook=false");
//		activeMQConnectionFactory.setOptimizedMessageDispatch(true); // don't know - possibly defaut anyway
//		activeMQConnectionFactory.setObjectMessageSerializationDefered(true); // do not serialise on send - only use object once 
//		activeMQConnectionFactory.setCopyMessageOnSend(false); // only use a message once
//		connectionFactory = activeMQConnectionFactory;
		JJMSConnectionFactory jjmsConnectionFactory = new JJMSConnectionFactory();
		jjmsConnectionFactory.run();
		connectionFactory = jjmsConnectionFactory;
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
		camelContext.addRoutes(new RouteBuilder() {
		    public void configure() {
		        from("test-jms:queue:test.queue").to("bean:munger");
		    }
		});

		client = ProxyHelper.createProxy(camelContext.getEndpoint("test-jms:queue:test.queue"), Munger.class);
		//client = ProxyHelper.createProxy(camelContext.getEndpoint("bean:munger"), Munger.class);
		camelContext.start();
		
		
	}

	protected void tearDown() throws Exception {
		camelContext.stop();
		super.tearDown();
	}

	public static class Unserialisable implements Serializable {
		private String serialisable = "unserialisable";
		
		public String toString() {
			return "<" + getClass().getSimpleName() + ": " + serialisable + ">"; 
		}

		private void writeObject(ObjectOutputStream out) throws IOException {
			throw new RuntimeException();
		}
	};
	
	public static interface Munger {
		public String munge(String string);
		public String munge(Munger munger, String string);
		public Unserialisable noop(Unserialisable unserialisable);
		public int one(Object object);
		
	}
	
	public static class MungerImpl implements Munger {
		public String munge(String string) {
			System.out.println("munging: " + string);
			return string.toUpperCase();
		}
		public String munge(Munger munger, String string) {
			return munger.munge(string);
		}
		
		public Unserialisable noop(Unserialisable unserialisable) {
			System.out.println("nooping: " + unserialisable);
			return unserialisable;
		}

		public int one(Object object) {
			System.out.println("one: " + object);
			//new Exception().printStackTrace();
			return 1;
		}
	}
	
	public void testBeanToBeanOverQueue() throws Exception {

		assertTrue(client.munge(string).equals(mungedString));
		logger.info("1:{}, 2:{}", "a", "b");
	}

	public void testProxyRemotability() throws Exception {
		// can we migrate a proxy and still use it ?
		//assertTrue(proxy.munge(proxy, string).equals(mungedString));
		// TODO: CAMEL proxies are not relocatable (Serialisable)... - can I replace their impl ?
	}
	
	public void testAMQ() throws Exception {
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
		message.setObject(new Unserialisable());
		producer.send(message);
		assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
	}
	
	public void testOutboundUnserialisable() throws Exception {
		Unserialisable unserialisable = new Unserialisable();
		String serialisable = "serialisable";
		//assertTrue(1 == server.one(serialisable));
		//assertTrue(1 == client.one(serialisable));
		logger.info("serialisable OK");
		//assertTrue(1 == server.one(unserialisable));
		assertTrue(1 == client.one(unserialisable));
		logger.info("unserialisable OK");
	}

	public void testCanWeAvoidBeingSerialised() throws Exception {
		Unserialisable unserialisable = new Unserialisable();
		assertTrue(unserialisable == server.noop(unserialisable));
		assertTrue(unserialisable == client.noop(unserialisable));
	}

	
}
