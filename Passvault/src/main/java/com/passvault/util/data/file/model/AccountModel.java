package com.passvault.util.data.file.model;

public class AccountModel {

	private String AccountName;
	private String UserName;
	private String Password;
	private String OldPassword;
	private String URL;
	private long UpdateTime;
	private boolean deleted;
	
	public String getAccountName() {
		return AccountName;
	}
	public void setAccountName(String accountName) {
		AccountName = accountName;
	}
	public String getUserName() {
		return UserName;
	}
	public void setUserName(String userName) {
		UserName = userName;
	}
	public String getPassword() {
		return Password;
	}
	public void setPassword(String password) {
		Password = password;
	}
	public String getOldPassword() {
		return OldPassword;
	}
	public void setOldPassword(String oldPassword) {
		OldPassword = oldPassword;
	}
	public String getURL() {
		return URL;
	}
	public void setURL(String uRL) {
		URL = uRL;
	}
	public long getUpdateTime() {
		return UpdateTime;
	}
	public void setUpdateTime(long updateTime) {
		UpdateTime = updateTime;
	}
	public boolean isDeleted() {
		return deleted;
	}
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	
}
