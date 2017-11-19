package com.passvault.util.model.syncserver;

public class User {

	private String accountUUID;
	private String accountPassword;
	private long lastSync;
	private float format;
	private boolean locked;
	private long lockedTime;
	
	
	public User(String accountUUID, String accountPassword, long lastSync, float format) {
		super();
		this.accountUUID = accountUUID;
		this.accountPassword = accountPassword;
		this.lastSync = lastSync;
		this.format = format;
		locked = false;
		lockedTime = 0L;
	}
	
	public String getAccountUUID() {
		return accountUUID;
	}
	public void setAccountUUID(String accountUUID) {
		this.accountUUID = accountUUID;
	}
	public String getAccountPassword() {
		return accountPassword;
	}
	public void setAccountPassword(String accountPassword) {
		this.accountPassword = accountPassword;
	}
	public long getLastSync() {
		return lastSync;
	}
	public void setLastSync(long lastSync) {
		this.lastSync = lastSync;
	}
	public float getFormat() {
		return format;
	}
	public void setFormat(float format) {
		this.format = format;
	}
	public boolean isLocked() {
		return locked;
	}
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public long getLockedTime() {
		return lockedTime;
	}

	public void setLockedTime(long lockedTime) {
		this.lockedTime = lockedTime;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return accountUUID + ", " + lastSync + ", " + locked;
	}
	
	
}
