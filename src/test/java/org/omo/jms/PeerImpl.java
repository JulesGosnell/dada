/**
 * 
 */
package org.omo.jms;


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