package com.passvault.util.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class RegistrationServer {

	private String registrationServer;

	public String getRegistrationServer() {
		return registrationServer;
	}

	public void setRegistrationServer(String registrationServer) {
		this.registrationServer = registrationServer;
	}
	
	
}
