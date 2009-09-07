package org.omo.cash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ManagerImpl<I extends Identifiable, T extends Identifiable> implements Manager<I, T> {

	protected final Log log = LogFactory.getLog(getClass());

	protected I identity;
	protected Map<Integer, T> id2T = new HashMap<Integer, T>();
	protected Set<Listener<T>> listeners = new HashSet<Listener<T>>();

	
	public ManagerImpl(I owner) {
		this.identity = owner;
	}

	@Override
	public int getId() {
		return identity.getId();
	}
	
	@Override
	public List<T> fetch(List<Integer> ids) {
		List<T> ts = new ArrayList<T>();
		for (int id : ids)
			ts.add(id2T.get(id));
		return ts;
	}

	@Override
	public void update(List<T> updates) {
		if (updates.size()>0)
			for (T update: updates)
				update(update);
	}

	@Override
	public void update(T newValue) {
		T oldValue = id2T.put(newValue.getId(), newValue);
		for (Listener<T> listener : listeners)
			listener.update(oldValue, newValue);
	}
	
	@Override
	public void register(Listener<T> listener) {
		listeners.add(listener);
	}

	@Override
	public T fetch(int id) {
		return id2T.get(id);
	}

	@Override
	public int size() {
		return id2T.size();
	}

}
