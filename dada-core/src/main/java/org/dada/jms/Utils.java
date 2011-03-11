package org.dada.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import org.dada.core.ClassLoadingAwareObjectInputStream;

public class Utils {
	
	public static Object readObject(BytesMessage message) throws JMSException, IOException, ClassNotFoundException {
		int length = (int)message.getBodyLength();
		byte[] bytes = new byte[length];

		int read = message.readBytes(bytes);
		if (read < length)
			System.out.println("" + (length - read) + " bytes short !!");

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ClassLoadingAwareObjectInputStream(bais);
		try {
			return ois.readObject();
		} finally {
			ois.close();
			bais.close();
		}
	}
	
	public static void writeObject(BytesMessage message, Object object) throws JMSException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		try {
			oos.writeObject(object);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oos.close();
			baos.close();
		}
		byte[] bytes = baos.toByteArray();
		message.writeBytes(bytes);
	}
}
