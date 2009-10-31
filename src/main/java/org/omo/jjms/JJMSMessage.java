package org.omo.jjms;

import java.rmi.server.UID;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

public class JJMSMessage implements Message {

	private String jmsType;
	private long jmsTimestamp;
	private long jmsExpiration;
	private Destination jmsDestination;
	private int jmsDeliveryMode; 
	private int jmsPriority; 
	private String jmsCorrelationId;
	private String jmsMessageID = new UID().toString(); // TODO: optimise - do we need this ? 
	private Destination jmsReplyTo;
	private final Map<String, Object> properties = new HashMap<String, Object>(); // TODO: allocate lazily
	
	public String toString() {
		return "<" + getClass().getSimpleName() + ">";
	}

	// JMS
	
	@Override
	public void acknowledge() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void clearBody() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void clearProperties() throws JMSException {
		properties.clear();
	}

	@Override
	public boolean getBooleanProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public byte getByteProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public double getDoubleProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public float getFloatProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public int getIntProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		return jmsCorrelationId;
	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		return jmsDeliveryMode;
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		return jmsDestination;
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		return jmsExpiration;
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return jmsMessageID;
	}

	@Override
	public int getJMSPriority() throws JMSException {
		return jmsPriority;
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return false; // not supported
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return jmsReplyTo;
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		return jmsTimestamp;
	}

	@Override
	public String getJMSType() throws JMSException {
		return jmsType;
	}

	@Override
	public long getLongProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Object getObjectProperty(String key) throws JMSException {
		return properties.get(key);
	}

	@Override
	public Enumeration getPropertyNames() throws JMSException {
		return new Enumeration<String>() {

			private Iterator<String> iterator = properties.keySet().iterator();

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next();
			}
		};
	}

	@Override
	public short getShortProperty(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getStringProperty(String key) throws JMSException {
		return (String)properties.get(key);
	}

	@Override
	public boolean propertyExists(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setBooleanProperty(String arg0, boolean arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setByteProperty(String arg0, byte arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setDoubleProperty(String arg0, double arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setFloatProperty(String arg0, float arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setIntProperty(String arg0, int arg1) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setJMSCorrelationID(String jmsCorrelationId) throws JMSException {
		this.jmsCorrelationId = jmsCorrelationId;
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setJMSDeliveryMode(int arg0) throws JMSException {
		this.jmsDeliveryMode = jmsDeliveryMode;
	}

	@Override
	public void setJMSDestination(Destination arg0) throws JMSException {
		this.jmsDestination = jmsDestination;
	}

	@Override
	public void setJMSExpiration(long jmsExpiration) throws JMSException {
		this.jmsExpiration = jmsExpiration;
	}

	@Override
	public void setJMSMessageID(String arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setJMSPriority(int jmsPriority) throws JMSException {
		this.jmsPriority = jmsPriority;
	}

	@Override
	public void setJMSRedelivered(boolean arg0) throws JMSException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setJMSReplyTo(Destination jmsReplyTo) throws JMSException {
		this.jmsReplyTo = jmsReplyTo;
	}

	@Override
	public void setJMSTimestamp(long jmsTimestamp) throws JMSException {
		this.jmsTimestamp = jmsTimestamp;
	}

	@Override
	public void setJMSType(String jmsType) throws JMSException {
		this.jmsType = jmsType;
	}

	@Override
	public void setLongProperty(String arg0, long arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setObjectProperty(String key, Object value) throws JMSException {
		properties.put(key, value);
	}

	@Override
	public void setShortProperty(String arg0, short arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}

	@Override
	public void setStringProperty(String arg0, String arg1)
			throws JMSException {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("NYI");
			}
}
