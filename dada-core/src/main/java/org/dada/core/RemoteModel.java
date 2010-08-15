package org.dada.core;

import java.io.IOException;
import java.io.Serializable;

public class RemoteModel<K, V> implements Model<K, V>, Serializable {

	private final Metadata<K, V> metadata;
	private final String name;
	
	private transient SessionManager sessionManager;
	
	public RemoteModel(String name, Metadata<K, V> metadata) throws Exception {
		this.metadata = metadata;
		this.name = name;
		this.sessionManager = null; // peer should never be needed this side of the wire
		// TODO: we need to start a server for our host Model
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		sessionManager = SessionManagerHelper.getCurrentSessionManager();
		if (sessionManager == null) new Exception().printStackTrace();
	}
	 
	@Override
	public void start() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Data<V> registerView(View<V> view) {
		return (Data<V>) sessionManager.registerView(name, (View<Object>)view);
	}

	@Override
	public Data<V> deregisterView(View<V> view) {
		return (Data<V>) sessionManager.deregisterView(name, (View<Object>) view);
	}

	@Override
	public Metadata<K, V> getMetadata() {
		return metadata;
	}

	@Override
	public Data<V> getData() {
		return (Data<V>) sessionManager.getData(name);
	}

	public String toString() {
		return "<0x" + Integer.toHexString(System.identityHashCode(this)) + ":" + getClass().getSimpleName() + " " + name + ">";
	}

	@Override
	public V find(K key) {
		return (V)sessionManager.find(name, key);
	}
}
