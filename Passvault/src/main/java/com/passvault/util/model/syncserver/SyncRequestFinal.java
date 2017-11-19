package com.passvault.util.model.syncserver;

import java.util.List;

public class SyncRequestFinal {
	private String user;
	private String password;
	private long lockTime;
	List<Account> accounts;
	
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
	public long getLockTime() {
		return lockTime;
	}
	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}
	public List<Account> getAccounts() {
		return accounts;
	}
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	
	
}
