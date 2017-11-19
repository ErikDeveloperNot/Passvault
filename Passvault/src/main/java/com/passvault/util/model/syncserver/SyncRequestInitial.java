package com.passvault.util.model.syncserver;

import java.util.List;

public class SyncRequestInitial {

	private String user;
	private String password;
	private List<CheckAccount> accounts;
	
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public List<CheckAccount> getAccounts() {
		return accounts;
	}
	public void setAccounts(List<CheckAccount> accounts) {
		this.accounts = accounts;
	}
	
	
}
