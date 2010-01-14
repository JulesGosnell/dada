package org.dada.slf4j;

/**
 * Borne out of desperation since it seems crazy that SLF4J still does not appear to support a varargs syntax...
 * 
 * @author jules
 *
 */
public class Logger {

	private final org.slf4j.Logger logger;
	
	protected Logger(org.slf4j.Logger logger) {
		this.logger = logger;
	}
	
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}
	
	public void trace(String string, Object... objects) {
		logger.trace(string, objects);
	}

	public void trace(String string, Throwable throwable) {
		logger.trace(string, throwable);
	}

	public void debug(String string, Object... objects) {
		logger.debug(string, objects);
	}

	public void debug(String string, Throwable throwable) {
		logger.debug(string, throwable);
	}

	public void info(String string, Object... objects) {
		logger.info(string, objects);
	}

	public void info(String string, Throwable throwable) {
		logger.info(string, throwable);
	}

	public void warn(String string, Object... objects) {
		logger.warn(string, objects);
	}

	public void warn(String string, Throwable throwable) {
		logger.warn(string, throwable);
	}

	public void error(String string, Object... objects) {
		logger.error(string, objects);
	}

	public void error(String string, Throwable throwable) {
		logger.error(string, throwable);
	}

}
