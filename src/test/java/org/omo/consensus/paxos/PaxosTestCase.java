package org.omo.consensus.paxos;

import java.io.IOException;

import org.omo.Utils;
import org.omo.consensus.paxos.Acceptor;
import org.omo.consensus.paxos.AcceptorImpl;
import org.omo.consensus.paxos.Proposal;


import junit.framework.TestCase;

/**
 * First attempt of a Paxos implementation based on 'Paxos Made Simple' - Leslie Lamport = 01 Nov 2001, see docs...
 * 
 * @author jules
 *
 */
public class PaxosTestCase extends TestCase {
	
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
