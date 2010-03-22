/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}
	public void trace(String string) {
		logger.trace(string);
	}
	public void trace(String string, Throwable throwable) {
		logger.trace(string, throwable);
	}
	public void trace(String string, Throwable throwable, Object... objects) {
		logger.trace(string, objects);
		logger.trace("", throwable);
	}
	public void trace(String string, Object... objects) {
		logger.trace(string, objects);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}
	public void debug(String string) {
		logger.debug(string);
	}
	public void debug(String string, Throwable throwable) {
		logger.debug(string, throwable);
	}
	public void debug(String string, Throwable throwable, Object... objects) {
		logger.debug(string, objects);
		logger.debug("", throwable);
	}
	public void debug(String string, Object... objects) {
		logger.debug(string, objects);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}
	public void info(String string) {
		logger.info(string);
	}
	public void info(String string, Object... objects) {
		logger.info(string, objects);
	}
	public void info(String string, Throwable throwable, Object... objects) {
		logger.info(string, objects);
		logger.info("", throwable);
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}
	public void warn(String string) {
		logger.warn(string);
	}
	public void warn(String string, Object... objects) {
		logger.warn(string, objects);
	}
	public void warn(String string, Throwable throwable, Object... objects) {
		logger.warn(string, objects);
		logger.warn("", throwable);
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}
	public void error(String string) {
		logger.error(string);
	}
	public void error(String string, Object... objects) {
		logger.error(string, objects);
	}
	public void error(String string, Throwable throwable, Object... objects) {
		logger.error(string, objects);
		logger.error("", throwable);
	}

}
