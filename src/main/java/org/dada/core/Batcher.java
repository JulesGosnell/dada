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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: a little naive, but lets go with it...

public class Batcher<K, V> extends Connector<K, V, K, V> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final int maxSize;
	private final long maxDelay;

	private final Timer timer = new Timer(true);
	
	private TimerTask task = null;
	
	private Collection<Update<V>> newInsertions;
	private Collection<Update<V>> newUpdates;
	private Collection<Update<V>> newDeletions;

	public Batcher(int maxSize, long maxDelay, Collection<View<K, V>> views) {
		super(views);
		this.maxSize = maxSize;
		this.maxDelay = maxDelay;
		newInsertions = new ArrayList<Update<V>>();
		newUpdates = new ArrayList<Update<V>>();
		newDeletions = new ArrayList<Update<V>>();
	}

	@Override
	public synchronized void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		if (insertions.size() + updates.size() + deletions.size() == 0)
			return;
		
		boolean empty = newInsertions.size() + newUpdates.size() + newDeletions.size() == 0;
		
		if (insertions.size() + updates.size() + deletions.size() > maxSize) {
			if (empty) {
				// no data standing by and enough incoming to be passed straight through...
				logger.trace("pass update through");
				notifyViews(insertions, updates, deletions);
			} else {
				// merge with outstanding data and send upstream...
				logger.trace("aggregate update and flush");
				add(insertions, updates, deletions);
				flush();
			}
		} else {
			// merge with outstanding data
			logger.trace("aggregate update");
			add(insertions, updates, deletions);
			// if big enough send upstream...	
			if (newInsertions.size() + newUpdates.size() + newDeletions.size() > maxSize) {
				logger.trace("size induced flush");
				flush();
			} else {
				// otherwise, if we had no outstanding data, set a timeout
				if (empty) {
					logger.trace("scheduling flush in {} millis", maxDelay);
					timer.schedule(task = new TimerTask() {
						@Override
						public void run() {
							logger.trace("timer induced flush");
							flush();
						}
					}
					, maxDelay);
				}
			}
		}
	}

	protected synchronized void flush() {

		notifyViews(newInsertions, newUpdates, newDeletions);

		newInsertions = new ArrayList<Update<V>>();
		newUpdates = new ArrayList<Update<V>>();
		newDeletions = new ArrayList<Update<V>>();

		logger.trace("cancelling timer");
		task.cancel();
		task = null;
	}
	
	protected void add(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		newInsertions.addAll(insertions);
		newUpdates.addAll(updates);
		newDeletions.addAll(deletions);
	}

	protected void notifyViews(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		for (View<K, V> view : getViews()) {
			view.update(insertions, updates, deletions);
		}
	}	

}
