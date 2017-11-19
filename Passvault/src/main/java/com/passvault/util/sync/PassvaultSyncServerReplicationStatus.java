package com.passvault.util.sync;

public class PassvaultSyncServerReplicationStatus implements ReplicationStatus {
	
	private String pullError;
	private String pushError;
	private boolean isRunning;
	
	public PassvaultSyncServerReplicationStatus(String pullError, String pushError, boolean isRunning) {
		super();
		this.pullError = pullError;
		this.pushError = pushError;
		this.isRunning = isRunning;
	}
	
	public PassvaultSyncServerReplicationStatus() {
		this(null, null, true);
	}

	public void setPullError(String pullError) {
		this.pullError = pullError;
	}

	public void setPushError(String pushError) {
		this.pushError = pushError;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public Throwable getPullError() {
		if (pullError == null || pullError.equalsIgnoreCase("")) {
			return null;
		} else {
			return new Exception(pullError);
		}
	}

	
	@Override
	public Throwable getPushError() {
		if (pushError == null || pushError.equalsIgnoreCase("")) {
			return null;
		} else {
			return new Exception(pushError);
		}
	}

}
