package com.passvault.util;

import java.io.Serializable;
import java.net.URI;

public class Account implements Comparable<Account>, Serializable {

	private String name;
	private String user;
	private String pass;
	private String oldPass;
	private String accountUUID;
	private long updateTime;
	private String url;
	
	public static final String BLANK_PASSWORD = "\t";
	
    //TODO, don't allow these special characters in password
	public static final String RECORD_DELIMETER = "\\|";
	public static final String FIELD_DELIMIETER = ":";
	
	public Account(String name, String user, String pass, String oldPass, String accountUUID, long updateTime,
			String url) {
		this.user = user;
		this.name = name;
		this.pass = pass;
		this.oldPass = oldPass;
		this.updateTime = updateTime;
		this.accountUUID = accountUUID;
		this.setUrl(url);
	}
	
	
	public Account(String name, String user, String pass, String oldPass, String accountUUID, long updateTime) {
		this(name, user, pass, oldPass, accountUUID, updateTime, "");
	}
	
	public String getAccountUUID() {
		return accountUUID;
	}


	public void setAccountUUID(String accountUUID) {
		this.accountUUID = accountUUID;
	}


	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	/*
	public Account(String name, String user, String pass, String accountUUID, long updateTime) {
		this(name, user, pass, pass, accountUUID, updateTime);
	}
	*/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
	
	public String getOldPass() {
		return oldPass;
	}

	public void setOldPass(String oldPass) {
		this.oldPass = oldPass;
	}

	@Override
	public int compareTo(Account o) {
		return name.toLowerCase().compareTo(o.getName().toLowerCase());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (((Account)obj).getName().equalsIgnoreCase(name))
			return true;
		else 
			return false;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		
		if (!url.toLowerCase().trim().startsWith("http")) 
			url = "http://" + url;
		
		this.url = url;
	}
	
	
}
