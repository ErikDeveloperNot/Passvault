package com.passvault.data.file.model;

import java.util.Map;

public class Generator {

	private boolean overRide;
	private Properties properties;
	
	public Generator() {
		overRide = false;
		properties = new Properties();
	}
	
	public Generator(Map<String, Object> values) {
		overRide = (Boolean)values.get("overRide");
		properties = new Properties((Map)values.get("properties"));
	}
	
	public boolean isOverRide() {
		return overRide;
	}
	public void setOverRide(boolean overRide) {
		this.overRide = overRide;
	}
	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "{\n overRide: " + overRide + "\n properties: " + properties.toString() + "\n}";
	}
	
	
}
