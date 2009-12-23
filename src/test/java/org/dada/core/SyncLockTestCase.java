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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SyncLockTestCase extends MockObjectTestCase {

	public void test() throws Exception {
		final Sync sync = mock(Sync.class);
		Lock lock = new SyncLock(sync);
		
		// unsuccessful sync acquisition...

		checking(new Expectations(){{
			one(sync).acquire();
            will(throwException(new InterruptedException()));
        }});
		
		try {lock.lock();fail();} catch (Exception e) {}
		
		// successful lock acquisition
		
		checking(new Expectations(){{
			one(sync).acquire();
        }});

		lock.lock();
		
		// successful lock release

		checking(new Expectations(){{
			one(sync).release();
        }});

		lock.unlock();

		// NYI
		
		try {lock.lockInterruptibly();        fail();} catch (UnsupportedOperationException e) {};
		try {lock.newCondition();             fail();} catch (UnsupportedOperationException e) {};
		try {lock.tryLock();                  fail();} catch (UnsupportedOperationException e) {};
		try {lock.tryLock(1, TimeUnit.HOURS); fail();} catch (UnsupportedOperationException e) {};
	}
}
