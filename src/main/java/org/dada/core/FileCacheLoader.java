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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileCacheLoader<K, V> extends AbstractModel<K, V> implements Loader<K, V>, View<V> {

	private final Collection<Update<V>> nil = Collections.emptyList();
	private final List<V> data = new ArrayList<V>();

	private final File file;
	private final Loader<K, V> loader;
	
	public FileCacheLoader(String name, Metadata<K, V> metadata, File file, Loader<K, V> loader) {
		super(name, metadata);
		this.file = file;
		this.loader = loader;
	}

	@Override
	public Collection<V> getData() {
		return data;
	}

	// TODO - Exception handling...
	
	@Override
	public void start() {
		if (file.exists()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
				int numData = ois.readInt();
				for (int numDataRead = 0; numDataRead < numData; numDataRead++) {
					V datum = (V) ois.readObject();
					data.add(datum);
					notifyUpdate(Collections.singleton(new Update<V>(null, datum)), nil, nil);
				}
				ois.close();
			} catch (Exception e) {
				throw new RuntimeException("problem reading file: " + file, e);
			}
		} else {
			loader.registerView(this);
			loader.start();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
				final int numData = data.size();
				oos.writeInt(numData);
				for (V datum : data) {
					oos.writeObject(datum);
				}
				oos.close();
			} catch (Exception e) {
				throw new RuntimeException("problem reading file: " + file, e);
			}
			loader.stop();
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public void update(Collection<Update<V>> insertions, Collection<Update<V>> updates, Collection<Update<V>> deletions) {
		notifyUpdate(insertions, updates, deletions);
		for (Update<V> insertion : insertions) {
			data.add(insertion.getNewValue());
		}
	}

}
