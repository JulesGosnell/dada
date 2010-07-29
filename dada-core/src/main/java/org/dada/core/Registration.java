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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Registration<K, V> implements Serializable {

	private /*final*/ Metadata<K, V> metadata;
	private /*final*/ Collection<V> extant;
	private /*final*/ Collection<V> extinct;

	public Registration(Metadata<K, V> metadata, Collection<V> extant, Collection<V> extinct) {
		this.metadata = metadata;
		this.extant = (Collection<V>) (extant == null  ? Collections.emptyList() : extant);
		this.extinct = (Collection<V>) (extinct == null  ? Collections.emptyList() : extinct);
	}

	public Metadata<K, V> getMetadata() {
		return metadata;
	}

	public Collection<V> getData() {
		return extant;
	}

	public Collection<V> getExtinct() {
		return extinct;
	}

	// by managing our own serialisation here, we can take the opportunity to rewrite the container
	// being used to hold our extant...
	// this is useful because some clojure-1.1 and jdk-6 containers are NOT serialisable AND it reduces the
	// size of our serialisation because the containers type info is now implicit so need not be sent BUT it
	// IS a little unnecessary complexity...
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(metadata);
		out.writeInt(extant.size());
		for (V datum : extant)
			out.writeObject(datum);
		out.writeInt(extinct.size());
		for (V datum : extinct)
			out.writeObject(datum);
	}
	 
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		metadata = (Metadata<K, V>)in.readObject();
		{
			int length = in.readInt();
			extant = new ArrayList<V>(length);
			for (int i = 0; i < length ; i++)
				extant.add((V)in.readObject());
		}
		{
			int length = in.readInt();
			extinct = new ArrayList<V>(length);
			for (int i = 0; i < length ; i++)
				extinct.add((V)in.readObject());
		}
	}
}
