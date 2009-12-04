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
package org.dada.consensus.paxos;


public class AcceptorImpl<V> implements Acceptor<V> {

	int number = 0;
	Proposal<V> proposal = new Proposal<V>(0, null);

	/* (non-Javadoc)
	 * @see uk.org.gosnell.consensus.paxos.Acceptor#prepare(int)
	 */
	public synchronized int prepare(int number) {
		if (number > this.number) {
			this.number = number;
			return proposal.getNumber();
		} else {
			return -1;
		}
	}

	/* (non-Javadoc)
	 * @see uk.org.gosnell.consensus.paxos.Acceptor#accept(uk.org.gosnell.consensus.paxos.Proposal)
	 */
	public synchronized boolean accept(Proposal<V> proposal) {
		int number = proposal.getNumber();
		if (number >= this.number) {
			this.number = number;
			this.proposal = proposal;
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see uk.org.gosnell.consensus.paxos.Acceptor#getNumber()
	 */
	public int getNumber() {
		return number;
	}

	/* (non-Javadoc)
	 * @see uk.org.gosnell.consensus.paxos.Acceptor#getProposal()
	 */
	public Proposal<V> getProposal() {
		return proposal;
	}

}
