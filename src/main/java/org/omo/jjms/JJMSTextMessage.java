package org.omo.jjms;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class JJMSTextMessage extends JJMSMessage implements TextMessage {

	private String text;
	
	@Override
	public String getText() throws JMSException {
		return text;
	}

	@Override
	public void setText(String text) throws JMSException {
		this.text = text;
	}

}
