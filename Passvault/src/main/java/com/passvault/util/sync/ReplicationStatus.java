package com.passvault.util.sync;

public interface ReplicationStatus {

	boolean isRunning();
	Throwable getPullError();
	Throwable getPushError();
}
