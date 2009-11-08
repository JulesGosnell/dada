/**
 * 
 */
package org.omo.jms;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Unserialisable implements Serializable {
	private String serialisable = "unserialisable";
	
	public String toString() {
		return "<" + getClass().getSimpleName() + ": " + serialisable + ">"; 
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		throw new RuntimeException();
	}
}