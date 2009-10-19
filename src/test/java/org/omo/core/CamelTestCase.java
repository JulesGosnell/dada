package org.omo.core;

import javax.jms.ConnectionFactory;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;

public class CamelTestCase extends TestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public static interface Munger {
		public String munge(String string);
	}
	
	public static class MungerImpl implements Munger {
		public String munge(String string) {
			System.out.println("munging: " + string);
			return string.toUpperCase();
		}
	}
	
	public void testRemoting() throws Exception {
		MungerImpl munger = new MungerImpl();
		String string = "hello";
		String mungedString = munger.munge(string);
		
		JndiContext jndiContext = new JndiContext();
		jndiContext.bind("munger", munger);
		CamelContext camelContext = new DefaultCamelContext(jndiContext);
		
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useShutdownHook=false");
		camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

		camelContext.addRoutes(new RouteBuilder() {
		    public void configure() {
		        from("test-jms:queue:test.queue").to("bean:munger");
		    }
		});
		camelContext.start();


		Munger proxy = ProxyHelper.createProxy(camelContext.getEndpoint("test-jms:queue:test.queue"), Munger.class);
		assertTrue(proxy.munge(string).equals(mungedString));
		assertTrue(proxy.munge(string).equals(mungedString));
		
		camelContext.stop();
		
	}
}
