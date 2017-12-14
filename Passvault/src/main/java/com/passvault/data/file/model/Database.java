package com.passvault.data.file.model;

import java.util.Map;

public class Database {

	private boolean purge;
	// specifies the number of days to wait before purging deleted accounts, defaults to 30
	private int numberOfDaysBeforePurge;
	
	public Database() {
		purge = false;
		numberOfDaysBeforePurge = 30;
	}
	
	public Database(Map<String, Object> values) {
		this.purge = (Boolean)values.get("purge");
		this.numberOfDaysBeforePurge = (Integer)values.get("numberOfDaysBeforePurge");
	}

	public boolean isPurge() {
		return purge;
	}

	public void setPurge(boolean purge) {
		this.purge = purge;
	}

	public int getNumberOfDaysBeforePurge() {
		return numberOfDaysBeforePurge;
	}

	public void setNumberOfDaysBeforePurge(int numberOfDaysBeforePurge) {
		this.numberOfDaysBeforePurge = numberOfDaysBeforePurge;
	}

	@Override
	public String toString() {
		return "{\n purge: " + purge + "\n numberOfDaysBeforePurge: " + numberOfDaysBeforePurge + "\n}";
	}
	
	
}
