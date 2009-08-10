package com.nomura.consensus.paxos;

public interface Proposer<V> {

	// THOUGHTS:
	
	// we probably need e.g. Node<v> implements Acceptor<V>, Proposer<V>, Learner<V>....
	// the result of marshalling and demarshalling a Node is a JMSProxy to that Node
	// therefore every Node needs an associated Destination (temporary Queue?)
	
	// at startup everyone gets an Node  proxy that dispatches on a clusterwide topic
	// as you startup, you make an async invocation on this interface proposing List<Acceptor>{yourself} as the value (membership)
	// this triggers everyone who is listening into adding their current membership list to this value a re-proposing it....
	// when the dust settles everyone should should have a complete membership list
	// anytime a node becomes suspect (i.e. does not reply to a round quickly enough) the suspicious node can launch a membership round...
	

	// once membership is established we can start thinking about arranging partitions - or should that be done as part of the membership round ?
	
	
}
