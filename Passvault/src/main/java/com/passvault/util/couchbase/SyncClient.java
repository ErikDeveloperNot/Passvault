package com.passvault.util.couchbase;

import com.passvault.data.couchbase.SyncGatewayClient.ReplicationStatus;
import com.passvault.util.sync.AccountsChanged;

public interface SyncClient {

	void sync();
	SyncClient setAccountsChanged(AccountsChanged accountsChanged);
	ReplicationStatus getReplicationStatus();
}
