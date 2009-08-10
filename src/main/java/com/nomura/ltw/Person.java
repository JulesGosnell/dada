package com.nomura.ltw;

public interface Person extends Identifiable {

	public Person getFather();
	public void setFather(Person father);
	public Person getMother();
	public void setMother(Person mother);
	

}