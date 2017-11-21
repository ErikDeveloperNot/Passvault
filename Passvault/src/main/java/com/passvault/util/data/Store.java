package com.passvault.util.data;

import java.util.Collection;
import java.util.List;

import com.passvault.util.Account;
import com.passvault.util.AccountAccessMap;
import com.passvault.util.MRUComparator;
import com.passvault.util.data.file.model.Settings;
import com.passvault.util.sync.AccountsChanged;
import com.passvault.util.sync.ReplicationStatus;

public interface Store {

	ReplicationStatus syncAccounts(String host, String protocol, int port, String db);
	ReplicationStatus syncAccounts(String host, String protocol, int port, String db,
			AccountsChanged accountsChanged);
	ReplicationStatus syncAccounts(String host, String protocol, int port, String db,
			String user, String password, AccountsChanged accountsChanged);
	void updateAccountUUID(List<Account> accounts, String uuid);
	void saveAccount(Account account);
	void saveAccount(Account account, String key);
	void saveAccounts(List<Account> accounts);
	void saveAccounts(List<Account> accounts, String key);
	void loadAccounts(List<Account> accounts);
	void deleteAccount(String accountName);
	void deleteAccount(Account account);
	byte[] decodeString(String toDecode);
	byte[] encodeBytes(byte[] toEncode);
	void setEncryptionKey(String encryptionKey);
	void saveAccessMap(Collection<AccountAccessMap> values);
	Collection loadAccessMap();
	public void printConflicts();
	public void purgeDeletes();
	void saveSettings(Settings settings);
	Settings loadSettings();
}
