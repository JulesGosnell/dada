package org.dada.core;


public class SimpleModelView<K, V extends Datum<K>> extends VersionedModelView<K, V> {

	public SimpleModelView(String name, Metadata<K, V> metadata) {
		super(name,
			  metadata,
			  new Getter<K, V>() {@Override public K get(V value) {return value.getId();}},
		      new Getter<Integer, V>() {@Override public Integer get(V value) {return value.getVersion();}});
	}

}
