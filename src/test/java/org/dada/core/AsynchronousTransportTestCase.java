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

import java.util.concurrent.ExecutorService;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class AsynchronousTransportTestCase extends MockObjectTestCase {

	public static interface Target {
		void async();
		boolean sync();
	}
	
	public void test() throws Exception {
		final ExecutorService executorService = (ExecutorService)mock(ExecutorService.class);
		final Target target = (Target)mock(Target.class);

		AsynchronousTransport<Target> transport = new AsynchronousTransport<Target>(new Class<?>[]{Target.class}, executorService);
		Target proxy = transport.decouple(target);

		// sync call - dispatched directly onto test
		
        checking(new Expectations(){{
            one(target).sync();
        }});
		
        proxy.sync();
        
        // async call - dispatched onto executor service

        checking(new Expectations(){{
            one(executorService).execute(with(any(Runnable.class)));
        }});
		
        proxy.async();

        // client
        
        try { transport.client("test"); fail();} catch (UnsupportedOperationException e){}
        
        // server
        
        try { transport.server(target, "test"); fail();} catch (UnsupportedOperationException e){}
        
		// method declared on Object - dispatched directly on proxy

        assertTrue(!proxy.toString().equals(target.toString()));
        
        // broken invocation

        checking(new Expectations(){{
        	one(target).async();
        	will(throwException(new UnsupportedOperationException()));
        }});

        AsynchronousTransport<Target>.Invocation invocation = transport.createInvocation(target, target.getClass().getMethod("async", (Class<?>[])null), null);
        invocation.run();
	}
	
}
