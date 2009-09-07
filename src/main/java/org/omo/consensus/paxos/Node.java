package org.omo.consensus.paxos;

public interface Node<V> extends Acceptor<V>, Proposer<V>, Learner<V> {

}
