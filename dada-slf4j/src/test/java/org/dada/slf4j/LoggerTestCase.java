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

import org.jmock.integration.junit3.MockObjectTestCase;

public class LoggerTestCase extends MockObjectTestCase {
	
//	public void test() {
//		final org.slf4j.Logger slf4jLogger = mock(org.slf4j.Logger.class);
//		final Logger logger = new Logger(slf4jLogger);
//
//		final String string = "string";
//		final Object object = new Object();
//		final Throwable throwable = new Throwable();
//		
//		checking(new Expectations(){{
//            Object[] objects = new Object[]{object};
//			one(slf4jLogger).trace(string, objects);
//            one(slf4jLogger).trace(string, throwable);
//			one(slf4jLogger).debug(string, objects);
//            one(slf4jLogger).debug(string, throwable);
//			one(slf4jLogger).info(string, objects);
//            one(slf4jLogger).info(string, throwable);
//			one(slf4jLogger).warn(string, objects);
//            one(slf4jLogger).warn(string, throwable);
//			one(slf4jLogger).error(string, objects);
//            one(slf4jLogger).error(string, throwable);
//        }});
//		
//		logger.trace(string, object);
//		logger.trace(string, throwable);
//		logger.debug(string, object);
//		logger.debug(string, throwable);
//		logger.info(string, object);
//		logger.info(string, throwable);
//		logger.warn(string, object);
//		logger.warn(string, throwable);
//		logger.error(string, object);
//		logger.error(string, throwable);
//	}

	public void testSignatures() {
		Logger  logger = LoggerFactory.getLogger(getClass());
		logger.warn("0xO");
		logger.warn("1xO: {} ", new Object());
		logger.warn("2xO: {} {}", new Object(), new Object());
		logger.warn("3xO: {} {} {}", new Object(), new Object(), new Object());
		logger.warn("4xO: {} {} {} {}", new Object(), new Object(), new Object(), new Object());
		logger.warn("1xT+0xO", new Exception());
		logger.warn("1xT+1xO: {}", new Exception(), new Object());
		logger.warn("1xT+2xO: {} {}", new Exception(), new Object(), new Object());
		logger.warn("1xT+3xO: {} {} {}", new Exception(), new Object(), new Object(), new Object());
		logger.warn("1xT+4xO: {} {} {} {}", new Exception(), new Object(), new Object(), new Object(), new Object());
	}
}
