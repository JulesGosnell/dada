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
package org.dada.jms;


public class PeerImpl implements Peer {

	@Override
	public int hashcode(final String string) {
		return string.hashCode();
	}

	@Override
	public void throwException() throws PeerException {
		throw new PeerException();
	}

	@Override
	public Exception returnException() {
		return new PeerException();
	}

	@Override
	public Object register(final Peer peer, final Object data) {
		return peer.callback(data);
	}

	@Override
	public String munge(final String string) {
		System.out.println("munging: " + string);
		//new Exception().printStackTrace();
		return string.toUpperCase();
	}

	@Override
	public String remunge(final Peer peer, final String string) {
		return peer.munge(string);
	}

	@Override
	public Unserialisable noop(final Unserialisable unserialisable) {
		System.out.println("nooping: " + unserialisable);
		return unserialisable;
	}

	@Override
	public int one(final Object object) {
		System.out.println("one: " + object);
		//new Exception().printStackTrace();
		return 1;
	}

	@Override
	public Object callback(final Object data) {
		return data;
	}

}
