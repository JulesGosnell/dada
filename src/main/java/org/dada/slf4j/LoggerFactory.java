package org.dada.slf4j;

/**
 * See comment in Logger
 * 
 * @author jules
 *
 */
public class LoggerFactory {

	public static Logger getLogger(Class clazz) {
		return new Logger(org.slf4j.LoggerFactory.getLogger(clazz));
	}
	
}
