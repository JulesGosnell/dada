package com.nomura.cash;

import java.util.List;

public interface Manager<I extends Identifiable, T extends Identifiable> extends Identifiable {

	void register(Listener<T> listener);
	
	// called from downstream...
	List<T> fetch(List<Integer> ids);
	T fetch(int id);
	int size();
	
	// called from upstream...
	void update(List<T> updates);
	void update(T update);

}