package org.omo.old;

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
import javax.jms.Topic;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO
// AM aggregates trade totals
// a CM which aggregages A totals
// a UI ? - copy Trevor !!

// query currencies by business
// query currencies
// query accounts by currency
// query trades by id
// etc...

public class CashTestCase extends TestCase {

	protected final static Log LOG = LogFactory.getLog(CashTestCase.class);
	protected Connection connection;
	protected Session session;
	protected Topic tradeTopic;
	protected MessageConsumer tradeManagerTradeConsumer;
	protected MessageConsumer accountManagerAccountConsumer;
	protected Topic accountTopic;
	protected TradeManager tradeManager;
	protected AccountManager accountManager;
	protected CountDownLatch latch;
	protected MessageProducer tradeProducer;
	protected MessageProducer accountProducer;
	
	public ConnectionFactory getConnectionFactory() throws JMSException {
		return new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false"); 
	}
	
	@Override
	protected void setUp() throws Exception {
		ConnectionFactory connectionFactory = getConnectionFactory();
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		tradeTopic = session.createTemporaryTopic();
		accountTopic = session.createTemporaryTopic();
		tradeManager = new TradeManagerImpl(new Identifiable() {
			
			@Override
			public int getId() {
				return 0;
			}
		});
		int accountId = 0;
		int currencyId = 0;
		Account account = new AccountImpl(accountId);
		accountManager = new AccountManagerImpl(account);
		latch = new CountDownLatch(2);
		tradeManagerTradeConsumer = session.createConsumer(tradeTopic);
		tradeManagerTradeConsumer.setMessageListener(new MessageListener() {
			
			@Override
			public void onMessage(Message message) {
				ObjectMessage objectMessage = (ObjectMessage)message;
				try {
					Update<Trade> update = (Update<Trade>)objectMessage.getObject();
					LOG.info("TRADE UPDATE: " + update);
					tradeManager.update(update.getUpdate());
				} catch (JMSException e) {
					LOG.error("problem retrieving Trade update from message", e);
				}
				latch.countDown();
			}
		});
		accountManagerAccountConsumer = session.createConsumer(accountTopic);
		accountManagerAccountConsumer.setMessageListener(new MessageListener() {
			
			@Override
			public void onMessage(Message message) {
				ObjectMessage objectMessage = (ObjectMessage)message;
				try {
					Update<Trade> update = (Update<Trade>)objectMessage.getObject();
					LOG.info("ACCOUNT UPDATE: " + update);
					accountManager.update(update.getUpdate());
				} catch (JMSException e) {
					LOG.error("problem retrieving Trade update from message", e);
				}
				latch.countDown();
			}
		});
		tradeProducer = session.createProducer(tradeTopic);
		accountProducer = session.createProducer(accountTopic);
	}

	protected void tearDown() {
		try {
			tradeManagerTradeConsumer.close();
			accountManagerAccountConsumer.close();
			tradeProducer.close();
			accountProducer.close();
			session.close();
			connection.stop();
			connection.close();
		} catch (Throwable t) {
			LOG.warn("unexpected problem tidying up", t);
		}
	}

	public void testSendReceive() throws SecurityException, NoSuchMethodException, JMSException, InterruptedException {

		int accountId = 0;
		int counterpartyId = 0;
		int currencyId = 0;
		int tradeId = 0;
		int amount = 100;
		Trade trade = new TradeImpl(tradeId, amount);

		assertTrue(tradeManager.fetch(tradeId) == null);
		assertTrue(accountManager.fetch(accountId) == null);
		{
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(new Update<Trade>(trade));
			tradeProducer.send(objectMessage);
		}
		{
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(new Update<Trade>(trade));
			accountProducer.send(objectMessage);
		}
		latch.await(5, TimeUnit.SECONDS);

		assertTrue(tradeManager.fetch(tradeId) != null);
		assertTrue(accountManager.fetch(tradeId) != null);
	}
}
