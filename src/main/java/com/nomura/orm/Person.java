/**
 * 
 */
package com.nomura.orm;

import java.util.List;

public interface Person extends Identifiable {
	
	Identifiable getFather();
	Identifiable getMother();
	List<Person> getChildren(); 
	
}