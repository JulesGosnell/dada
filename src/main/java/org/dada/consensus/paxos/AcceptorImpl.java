package org.omo.consensus.paxos;


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
