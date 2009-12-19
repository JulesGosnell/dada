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
package org.dada.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Transport that returns a proxy for its target, invocations upon which are queued and executed on another thread.
 * It may be sensible to execute invocations returning values directly on the calling Thread.
 *  
 * @author jules
 *
 * @param <T>
 */
public class AsynchronousTransport<T> implements Transport<T> {

	private final Logger log = LoggerFactory.getLogger(AsynchronousTransport.class);
	
	private final ExecutorService executorService;
	private final Class<?>[] interfaces;
	
	public AsynchronousTransport(Class<?>[] interfaces, ExecutorService executorService) {
		this.interfaces = interfaces;
		this.executorService = executorService;
	}

	private class Invocation implements Runnable {

		private final T target;
		private final Method method;
		private final Object[] args;
		
		protected Invocation(T target, Method method, Object[] args) {
			this.target = target;
			this.method = method;
			this.args = args;
		}
		
		@Override
		public void run() {
			try {
				method.invoke(target, args);
			} catch (Exception e) {
				log.error("problem during async invocation ({})", this, e);
			}
		}
		
		@Override
		public String toString() {
			return "<" + getClass().getSimpleName() + ":" + target + "." + method + ": " + Arrays.toString(args) + ">";
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T decouple(final T target) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getReturnType() == Void.TYPE && !method.getDeclaringClass().equals(Object.class)) {
					// async invocation
					executorService.execute(new Invocation(target, method, args));
					return null;
				} else {
					// sync invocation
					return method.invoke(target, args);
				}
			}
		};
		return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, handler);
	}

}
