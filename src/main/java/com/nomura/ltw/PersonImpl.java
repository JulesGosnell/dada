package com.nomura.ltw;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PersonImpl extends IdentifiableImpl implements Person{

	private static final Log LOG = LogFactory.getLog(PersonImpl.class);

	private Person father;
	private Person mother;
	
	public PersonImpl(int id) {
		super(id);
	}
	
	@Override
	public Person getFather() {
		return father;
	}

	@Override
	public void setFather(Person father) {
		this.father = father;
	}

	@Override
	public Person getMother() {
		return mother;
	}

	@Override
	public void setMother(Person mother) {
		this.mother = mother;
	}


}
