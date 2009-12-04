package org.omo.ltw;


public class PersonImpl extends IdentifiableImpl implements Person{

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
