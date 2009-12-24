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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class AsynchronousTransportTestCase extends MockObjectTestCase {

	public static interface Target {
		
		void asyncVoidTest();
		void asyncExceptionTest() throws Exception;
		boolean syncTest();
	}
	
	public void testException() {
		// How do we define a mock object that calls a method on something passed to it...
		
		final ExecutorService executorService = new ExecutorService() {
			
			@Override
			public void execute(Runnable command) {
				command.run();
			}
			
			@Override
			public <T> Future<T> submit(Runnable task, T result) {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public Future<?> submit(Runnable task) {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public <T> Future<T> submit(Callable<T> task) {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public List<Runnable> shutdownNow() {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public void shutdown() {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public boolean isTerminated() {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public boolean isShutdown() {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
				throw new UnsupportedOperationException("NYI");
			}
			
			@Override
			public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
				throw new UnsupportedOperationException("NYI");
			}
		};

		final Target target = (Target)mock(Target.class);

		Transport<Target> transport = new AsynchronousTransport<Target>(new Class<?>[]{Target.class}, executorService);
		Target proxy = transport.decouple(target);
		
        // call a method on target that throws an exception
        checking(new Expectations(){{
        	try {
        		one(target).asyncExceptionTest();
        		will(throwException(new UnsupportedOperationException()));
        	} catch (Exception e) {
        		// ignore
        	}
        }});
        
        try {
        	proxy.asyncExceptionTest();
        	// since the exception is expected to be thrown on another thread, it is caught, reported and swallowed...
        	assertTrue(true);
        } catch (Exception e) {
        	assertTrue(false);
        }
	}
	
	public void test() throws Exception {
		final ExecutorService executorService = (ExecutorService)mock(ExecutorService.class);
		final Target target = (Target)mock(Target.class);

		Transport<Target> transport = new AsynchronousTransport<Target>(new Class<?>[]{Target.class}, executorService);
		Target proxy = transport.decouple(target);

		// sync call - dispatched directly onto test
		
        checking(new Expectations(){{
            one(target).syncTest();
        }});
		
        proxy.syncTest();
        
        // async call - dispatched onto executor service
        checking(new Expectations(){{
            one(executorService).execute(with(any(Runnable.class)));
        }});
		
        proxy.asyncVoidTest();

        // server
        
        try { transport.server(target, "test"); fail();} catch (UnsupportedOperationException e){}
        
	}
	
//	interface Empty {
//	}
//	
//	public void testMethodsDeclaredOnObject() {
//	
//		final String string = "toString";
//		Empty target = new Empty() {
//			@Override
//			String toString() {
//				return string;
//			}
//		}
//		
//	}
}
