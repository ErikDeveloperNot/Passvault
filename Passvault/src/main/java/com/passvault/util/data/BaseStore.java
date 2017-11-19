package com.passvault.util.data;

import java.util.Collection;
import java.util.List;

import com.passvault.util.Account;
import com.passvault.util.AccountAccessMap;
import com.passvault.util.sync.AccountsChanged;
import com.passvault.util.sync.ReplicationStatus;

import android.util.Base64;

/*
 * BaseStore is not a full implementation and only implements encode/decode String
 */
public abstract class BaseStore implements Store {
	
	abstract public ReplicationStatus syncAccounts(String host, String protocol, int port, String db);

	abstract public ReplicationStatus syncAccounts(String host, String protocol, int port, String db,
			AccountsChanged accountsChanged);
	
	abstract public ReplicationStatus syncAccounts(String host, String protocol, int port, String db, String user,
			String password, AccountsChanged accountsChanged);

	abstract public void updateAccountUUID(List<Account> accounts, String uuid);
	
	abstract public void saveAccount(Account account);
	
	abstract public void saveAccount(Account account, String key);
	
	abstract public void saveAccounts(List<Account> accounts);
	
	abstract public void saveAccounts(List<Account> accounts, String key);
	
	abstract public void loadAccounts(List<Account> accounts);

	abstract public void deleteAccount(String accountName);
	
	abstract public void deleteAccount(Account account);

	@Override
	public byte[] decodeString(String toDecode) {
		
		if (toDecode == null || toDecode.equals("")) {
			//return Base64.getDecoder().decode("");
			return Base64.decode("", Base64.DEFAULT);
		} else {
			//return Base64.getDecoder().decode(toDecode);
			return Base64.decode(toDecode, Base64.DEFAULT);
		}
	}

	@Override
	public byte[] encodeBytes(byte[] toEncode) {
		
		if (toEncode == null) {
			//return Base64.getEncoder().encode(new byte[]{});
			return Base64.encode(new byte[]{}, Base64.DEFAULT);
		} else {
			//return Base64.getEncoder().encode(toEncode);
			return Base64.encode(toEncode, Base64.DEFAULT);
		}
	}
	
	@Override
	public void purgeDeletes() {
		// default do nothing since this is specific to couch
	}
	
	@Override
	public void printConflicts() {
		// default do nothing since this is specific to couch
	}

	abstract public void setEncryptionKey(String encryptionKey);
	
	abstract public void saveAccessMap(Collection<AccountAccessMap> values);
	
	abstract public Collection loadAccessMap();
}
