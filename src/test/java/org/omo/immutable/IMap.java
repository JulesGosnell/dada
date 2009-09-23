package org.omo.immutable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface IMap<K, V> {

	public boolean containsKey(Object key);

	public boolean containsValue(Object value);

	public Set<java.util.Map.Entry<K, V>> entrySet();

	public V get(Object key);

	public boolean isEmpty();

	public Set<K> keySet();

	public int size();

	public Collection<V> values();

	public void clear();

	public V put(K key, V value);

	public void putAll(Map<? extends K, ? extends V> m);

	public V remove(Object key);

}