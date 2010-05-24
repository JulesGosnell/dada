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

import java.io.IOException;

import junit.framework.TestCase;

/**
 * First attempt of a Paxos implementation based on 'Paxos Made Simple' - Leslie Lamport = 01 Nov 2001, see docs...
 *
 * @author jules
 *
 */
public class PaxosTestCase extends TestCase {

	@SuppressWarnings("unchecked")
	public void testProposal() throws IOException, ClassNotFoundException {
		// does it construct correctly ?
		int number = 9;
		String value = "foo";
		Proposal<String> proposal = new Proposal<String>(number, value);
		assertEquals(number, proposal.getNumber());
		assertEquals(value, proposal.getValue());

		// is it properly serialisable ?
		Proposal<String> proposal2 = (Proposal<String>) Utils.deserialise(Utils.serialise(proposal));
		assertEquals(number, proposal2.getNumber());
		assertEquals(value, proposal2.getValue());
		assertNotSame(proposal, proposal2);

		// does it support equality correctly ?
		assertEquals(proposal, proposal2);
		assertFalse(proposal.equals(new Proposal<String>(number+1, value)));
		assertFalse(proposal.equals(new Proposal<String>(number, "bar")));
		assertEquals(proposal, new Proposal<String>(number, value));

	}


	public void testAcceptor() {
		Acceptor<String> acceptor = new AcceptorImpl<String>();

		// check initial state...
		int number = acceptor.getNumber();
		assertEquals(0, number);
		Proposal<String> proposal = acceptor.getProposal();
		assertEquals(number, proposal.getNumber());
		assertEquals(null, proposal.getValue());

		// a successful preparation
		number = 1;
		assertEquals(0, acceptor.prepare(number)); // 0 -> 1 - Yes
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());

		// an unsuccessful preparation - n too low
		assertEquals(-1, acceptor.prepare(0)); // 1 -> 0 - No
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());

		// a successful re-preparation
		number = 3;
		assertEquals(0, acceptor.prepare(number)); // 1 -> 3 - Yes
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());

		// an unsuccessful acceptance
		assertFalse(acceptor.accept(new Proposal<String>(2, ""))); // not accepted
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());

		// acceptance of an n that has been prepared
		proposal = new Proposal<String>(number, "");
		assertTrue(acceptor.accept(proposal)); // accepted
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());

		// acceptance of an n that has not been prepared (is this correct ?)
		number = 5;
		proposal = new Proposal<String>(number, "");
		assertTrue(acceptor.accept(proposal)); // accepted
		assertEquals(number, acceptor.getNumber());
		assertEquals(proposal, acceptor.getProposal());
	}

}
