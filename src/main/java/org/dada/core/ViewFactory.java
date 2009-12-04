/**
 * 
 */
package org.omo.core;

public interface ViewFactory<K, V> {
	View<K, V> create();
}