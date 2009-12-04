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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModel<K, V> implements Model<K, V> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final String name;

	protected final Collection<View<K, V>> views = new ArrayList<View<K, V>>();

	protected final Metadata<K, V> metadata;

	protected abstract Collection<V> getData();

	public AbstractModel(String name, Metadata<K, V> metadata) {
		this.name = name;
		this.metadata = metadata;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Registration<K, V> registerView(View<K, V> view) {
		logger.debug("registering view: {}", view);
		synchronized (views) {
			views.add(view);
		}
		return new Registration<K, V>(getMetadata(), getData());
	}

	private Metadata<K, V> getMetadata() {
		return metadata;
	}

	@Override
	public boolean deregisterView(View<K, V> view) {
		boolean success;
		synchronized (views) {
			success = views.remove(view);
		}
		if (success)
			logger.debug("deregistered view: {}", view);
		else
			logger.warn("failed to deregister view: {}", view);

		return success;
	}

	protected void notifyUpdates(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		for (View<K, V> view : views)
			try {
				view.update(insertions, updates, deletions);
			} catch (RuntimeException e) {
				logger.error("view notification failed: {} <- {}", view, updates);
				logger.error("", e);
			}
	}
}
