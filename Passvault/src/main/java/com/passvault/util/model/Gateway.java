package com.passvault.util.model;

import java.util.Map;

public class Gateway {

	/*
	        "server": "10.112.151.106",
            "port": "4984",
            "bucket": "passvault",
            "protocol": "http",
            "userName": "userA",
            "password": "password"
	 */
	
	private String server;
	private String bucket;
	private String protocol;
	private String userName;
	private String password;
	private int port;
	
	public Gateway() {
		
	}
	
	public Gateway(Map<String, Object> values) {
		if (values != null) {
			server = (String)values.get("server");
			bucket = (String)values.get("bucket");
			protocol = (String)values.get("protocol");
			userName = (String)values.get("userName");
			password = (String)values.get("password");
			port = (Integer)values.get("port");
		}
	}
	
	@Override
	public String toString() {
		return "Server: " + server + "\nport: " + port + "\nprotocol: " + protocol + "\ndatabase: " + bucket +
				"\nusername: " + userName;
	}
	
	
	@Override
	public boolean equals(Object obj) {

		if (obj == null || (obj instanceof Gateway == false))
			return false;
		
		if (obj.toString().equals(this.toString()))
			return true;
		
		return false;
	}


	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getBucket() {
		return bucket;
	}
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	
}
