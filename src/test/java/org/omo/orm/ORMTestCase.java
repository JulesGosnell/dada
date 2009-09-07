package org.omo.orm;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omo.orm.Person;

import junit.framework.TestCase;

public class ORMTestCase extends TestCase {
	
	// TODO an OO model of a family-tree that is also addressable by Id
	
	// relational layer...
	private final Map<Integer, Person>  idToPerson     = new HashMap<Integer, Person>();
	private final Map<Integer, Integer> personToFather = new HashMap<Integer, Integer>();
	private final Map<Integer, Integer> personToMother = new HashMap<Integer, Integer>();
	private final Map<Integer, Integer> personToChild  = new HashMap<Integer, Integer>();
	
	// OO layer...
	class PersonImpl implements Person {

		private final int id;

		public PersonImpl(int id) {
			this.id = id;
		}
		
		@Override
		public int getId() {
			return id;
		}

		@Override
		public Person getFather() {
			return idToPerson.get(personToFather.get(id));
		}

		public Person getMother() {
			return idToPerson.get(personToMother.get(id));
		}

		@Override
		public List<Person> getChildren() {
			// TODO: tricker - do we keep out own list of ids - a table scan on every call would be prohibitive
			// but if we maintain our own copy it will become out of date if we access the relational layer directly...
			return null;
		}
	}
	
	// I think we probably want to load data/accept updates (write) into the relational layer whilst
	// traversing and reporting from (reading) the OO layer...
	
	
	// PROBLEM:
	// looking up someone's grandfather via the object layer :
	//  person.getFather().getFather()
	// will involve 4 dehashes....
	// looking up someone's grandfather via the relational model :
	// idToPerson.get(personToFather.get(personToFather.get(person.getId())));
	// would only involve 3 dehashes...
	// rather than building an expensive cache aggressively and slowing down data loading, maybe we could build it lazily - but how could we arrange it's invalidation ?
	// it might contain 1000,000 objects...
	// the slower access times might not be a problem for 1;1 relationships - but 1:N and N:N might be a lot slower/trickier...
	// we could restrict writing to the OO layer aswell - but it might slow down loading to much - consider... - it would solve our invalidation issues though...
	// could we spike the oo layer with aspects to keep the relational layer up to date ? and do all all rw access via the object layer ?
	
	public void testORM() {
		assertTrue(true); // TODO - empty test
	}
}
