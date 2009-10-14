/**
 * 
 */
package org.omo.core;

import org.omo.core.ViewTestCase.BooleanDatum;

class StringDatum extends BooleanDatum {

	String string;
	
	StringDatum(int id, boolean flag, String string) {
		super(id, flag);
		this.string = string;
	}
}