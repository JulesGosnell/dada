/**
 * 
 */
package org.omo.cash;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SyncLock implements Lock {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Sync sync;
	private volatile int count;
	
	public SyncLock(Sync sync) {
		this.sync = sync;
	}
	
	@Override
	public void lock() {
		try {
			sync.acquire();
			logger.debug("{}: LOCKED -> {}", Thread.currentThread().getName(), ++count);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean tryLock() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void unlock() {
		sync.release();
		logger.debug("{}: UNLOCKED -> {}", Thread.currentThread().getName(), --count);
	}
	
}