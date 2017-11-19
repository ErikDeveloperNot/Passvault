package com.passvault.util.model.syncserver;

import java.util.List;

public class SyncResponseInitial {

	private int responseCode;
	private long lockTime;
	private List<String> sendAccountsToServerList;
	private List<Account> accountsToSendBackToClient;
	
	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public List<String> getSendAccountsToServerList() {
		return sendAccountsToServerList;
	}

	public void setSendAccountsToServerList(List<String> sendAccountsToServerList) {
		this.sendAccountsToServerList = sendAccountsToServerList;
	}

	public List<Account> getAccountsToSendBackToClient() {
		return accountsToSendBackToClient;
	}

	public void setAccountsToSendBackToClient(List<Account> accountsToSendBackToClient) {
		this.accountsToSendBackToClient = accountsToSendBackToClient;
	}

	public long getLockTime() {
		return lockTime;
	}

	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}
	
	
}
