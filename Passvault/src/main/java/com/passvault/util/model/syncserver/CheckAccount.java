package com.passvault.util.model.syncserver;

public class CheckAccount {
	private String accountName;
	private long updateTime;
	
	public CheckAccount(String accountName, long updateTime) {
		super();
		this.accountName = accountName;
		this.updateTime = updateTime;
	}
	
	public CheckAccount() {
		
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	/*
	@Override
	public boolean equals(Object obj) {
		boolean superResponse = super.equals(obj);
		
		System.out.println("Checking against object type: " + obj.getClass().getName());
		if (obj instanceof Account) {
			return ((Account)obj).getAccountName().equalsIgnoreCase(accountName) ? true : false;
		} else {
			return superResponse;
		}
	}
	*/
}
