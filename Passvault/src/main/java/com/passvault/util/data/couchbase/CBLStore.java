package com.passvault.util.data.couchbase;


import java.util.ArrayList;
import java.util.Collection;
//import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.JavaContext;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.passvault.crypto.AESEngine;
import com.passvault.crypto.CryptEngine;
import com.passvault.util.Account;
import com.passvault.util.MRUComparator;
import com.passvault.util.AccountAccessMap;
import com.passvault.util.Utils;
import com.passvault.util.data.BaseStore;
import com.passvault.util.sync.AccountsChanged;

import android.util.Base64;

public class CBLStore extends BaseStore {

	protected String databaseName;
	protected Database database;
	protected String databaseFormat;
	protected String encryptionKey;
	protected String accountUUID;
	
	private static Logger logger;
	private static String ACCESS_MAP_DTYPE = "access_map";
	public static enum DatabaseFormat {SQLite, ForestDB}
	
	
	static {
		logger = Logger.getLogger("com.passvault.util.couchbase");
	}

	

	protected CBLStore() {}
	
	public CBLStore(String databaseName, DatabaseFormat dbFormat, String key) {
		super();
		this.databaseName = databaseName;
		encryptionKey = key;
		
		logger.info("Creating CBLStore for: " + databaseName + ", format: " + dbFormat);
		
		switch (dbFormat) {
		case SQLite:
			databaseFormat = "SQLite";
			break;
		case ForestDB:
			databaseFormat = "ForestDB";
			break;
		default:
			databaseFormat = "SQLite";
			break;
		}
		
		DatabaseOptions dbOptions = new DatabaseOptions();
		dbOptions.setCreate(true);
		dbOptions.setStorageType(databaseFormat);
		dbOptions.setReadOnly(false);
		
		JavaContext context = new JavaContext();
		
		try {
			Manager manager = new Manager(context, Manager.DEFAULT_OPTIONS);
			database = manager.openDatabase(databaseName, dbOptions);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// set accountUUID if a remote sync config exists
		accountUUID = Utils.getAccountUUID();
		logger.info("Setting Account UUID to: " + accountUUID);
	}
	
	
	public SyncGatewayClient.ReplicationStatus syncAccounts(String host, String protocol, int port, String bucket) {
		SyncGatewayClient sgc = new SyncGatewayClient(database, host, protocol, port, bucket);
		sgc.sync();
		return sgc.getReplicationStatus();
	}
	
	
	public SyncGatewayClient.ReplicationStatus syncAccounts(String host, String protocol, int port, String bucket,
			Database.ChangeListener changeListener) {
		
		SyncGatewayClient sgc = new SyncGatewayClient(database, host, protocol, port, bucket);
		sgc.setDatabaseChangeListener(changeListener).sync();
		return sgc.getReplicationStatus();
	}
	
	
	public SyncGatewayClient.ReplicationStatus syncAccounts(String host, String protocol, int port, String bucket,
			AccountsChanged accountsChanged) {
		
		SyncGatewayClient sgc = new SyncGatewayClient(database, host, protocol, port, bucket);
		sgc.setAccountsChanged(accountsChanged).sync();
		return sgc.getReplicationStatus();
	}
	
	
	public SyncGatewayClient.ReplicationStatus syncAccounts(String host, String protocol, int port, String bucket,
			String user, String password, AccountsChanged accountsChanged) {
		
		SyncGatewayClient sgc = new SyncGatewayClient(database, host, protocol, port, bucket, user, password);
		sgc.setAccountsChanged(accountsChanged).sync();
		return sgc.getReplicationStatus();
	}
	
	
	public void updateAccountUUID(List<Account> accounts, String uuid) {
		/*
		List<Account> newAccounts = new ArrayList<>();

		synchronized (accounts) {
			for (Account account : accounts) {
				newAccounts.add(new Account(account.getName(), account.getUser(), account.getPass(), 
						account.getOldPass(), uuid, System.currentTimeMillis()));
				//saveAccount(account, oldUUID);
				deleteAccount(account); //, oldUUID);
			}
		}
		
		accounts = newAccounts;
		accountUUID = uuid;
		saveAccounts(accounts);
		*/
		
		// new implementation
		List<String> deleteAccounts = new ArrayList<>();
		List<Account> newAccounts = new ArrayList<>();
		
		synchronized (accounts) {
			for (Account account : accounts) {
				Account toAdd = new Account(account.getName(), account.getUser(), account.getPass(), 
						account.getOldPass(), uuid, account.getUpdateTime(), account.getUrl());
				toAdd.setValidEncryption(account.isValidEncryption());
				deleteAccounts.add(account.getName());
				newAccounts.add(toAdd);
			}
		}
		
		for (String accountName : deleteAccounts) {
			logger.finest("Deleting account: " + accountName);
			deleteAccount(accountName);
		}
		
		logger.info("Setting new Account UUID: " + uuid);
		accountUUID = uuid;
		logger.fine("Saving new accounts");
		saveAccounts(newAccounts);
	}
	
	
	public void saveAccount(Account account) {
		saveAccount(account, encryptionKey);
	}
	
	
	public void saveAccount(Account account, String key) {
		List<Account> addAccount = new ArrayList<>();
		addAccount.add(account);
		saveAccounts(addAccount, key); //, accountUUID);
	}
	
	/*
	public void saveAccount(Account account, String _accountUUID) {
		List<Account> addAccount = new ArrayList<>();
		addAccount.add(account);
		saveAccounts(addAccount, _accountUUID);
	}
	*/
	
	/*
	public void saveAccounts(List<Account> accounts) {
		saveAccounts(accounts, accountUUID);
	}
	*/
	
	
	public void saveAccounts(List<Account> accounts) {
		saveAccounts(accounts, encryptionKey);
	}
	
	
	public void saveAccounts(List<Account> accounts, String key) { //, String _accountUUID) {
		
		CryptEngine aesEngine = AESEngine.getInstance();
		logger.fine("Saving " + accounts.size() + " accounts");
		
		synchronized (accounts) {
			for (Account account : accounts) {
				final Map<String, Object> content = new HashMap<>();
			
				content.put("AccountName", account.getName());
				content.put("UserName", account.getUser());
				
				if (account.isValidEncryption()) {
					content.put("Password", 
							new String(encodeBytes(aesEngine.encryptString(key, account.getPass()))));
					content.put("OldPassword", 
							new String(encodeBytes(aesEngine.encryptString(key, account.getOldPass()))));
				} else {
					logger.warning("Saving account: " + account.getName() + ", but password couldn't be decrypted.");
					content.put("Password", account.getPass());
					content.put("OldPassword", account.getOldPass());
				}
				
				
				content.put("UpdateTime", account.getUpdateTime());
				//content.put("AccountUUID", account.getAccountUUID());
				content.put("AccountUUID", accountUUID);
				content.put("URL", account.getUrl());
				
				Document document = database.getDocument(accountUUID + account.getName());
				logger.finest("Document id: " + accountUUID + account.getName() + ", getCurrentRevisionId: " + 
						document.getCurrentRevisionId());				
				
				try {
					
					if (document.getCurrentRevisionId() != null) {
						document.update(new Document.DocumentUpdater() {
							
							@Override
							public boolean update(UnsavedRevision newRev) {
								newRev.setUserProperties(content);
								return true;
							}
						});
					} else {
						document.putProperties(content);
					}
			
				} catch (CouchbaseLiteException e) {
					System.err.println("unable to save account: " + account.getName());
					logger.log(Level.WARNING, "Unable to save account: " + account.getName() + ", error: " +
							e.getMessage(), e);
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	
	public void loadAccounts(List<Account> accounts) {//throws Exception {
		Query query = database.createAllDocumentsQuery();
		CryptEngine aesEngine = AESEngine.getInstance();
		logger.fine("Loading accounts");
		
		try {
			QueryEnumerator enumerator = query.run();
			QueryRow queryRow = null;
			
			while ((queryRow = enumerator.next()) != null) {
				boolean decrypted = true;
				
				Document document = queryRow.getDocument();
				
				if (document.getProperty("docType") != null)
					continue;
				
				String _accountUUID = (String)document.getProperty("AccountUUID");
				logger.finest("Retrieved account UUID from document: " + _accountUUID);
				
				if (_accountUUID == null)
					_accountUUID = "";
				
				// don't load docs if the accountUUID does not match current, will add purging later on
				if (!accountUUID.equalsIgnoreCase(_accountUUID))
					continue;
				
				String accountName = (String)document.getProperty("AccountName");
				String userName = (String)document.getProperty("UserName");
				
				String password = null;
				String oldPassword = null;
				
				try {
					password = aesEngine.decryptBytes(encryptionKey,
							decodeString(((String)document.getProperty("Password"))));
					oldPassword = aesEngine.decryptBytes(encryptionKey,
							decodeString(((String)document.getProperty("OldPassword"))));
				} catch (Exception e) {
					/*
					 * if password can't be decrypted mark account, if old password can't be decrypted
					 * chz around it by making current password the old one 
					 */
					if (password == null) {
						password = (String)document.getProperty("Password");
						oldPassword = (String)document.getProperty("OldPassword");
						decrypted = false;
						logger.warning("Unable to decrypt current password with Key, marking account: " + accountName);
					} else {
						oldPassword = password;
						logger.warning("Unable to decrypt old password, replacing with current password for account: " +
								accountName);
					}
				}
				
				long updateTime;
				Object updateObj = document.getProperty("UpdateTime");
				
				if (updateObj == null) {
					updateTime = System.currentTimeMillis();
				} else {
					if (updateObj instanceof Long)
						updateTime = (Long)updateObj;
					else if (updateObj instanceof Double)
						updateTime = ((Double)updateObj).longValue();
					else {
						System.err.println("UpdateTime could not be parsed, using current time, updateObject=" +
											updateObj.getClass().getName());
						updateTime = System.currentTimeMillis();
					}
				}
				
				String url = (String)document.getProperty("URL");
				
				/*
				String _accountUUID = (String)document.getProperty("AccountUUID");
				
				if (_accountUUID == null)
					_accountUUID = "";
				*/
				
				logger.finest("Adding account: " + accountName);
				Account account = new Account(accountName, userName, password, oldPassword, _accountUUID, updateTime,
						(url != null) ? url : "");
				accounts.add(account);
				
				if (!decrypted)
					account.setValidEncryption(false);
			}
			
		} catch (CouchbaseLiteException e) {
			logger.log(Level.WARNING, "Error loading accounts: " + e.getMessage(), e);
			e.printStackTrace();
		} /*catch (Exception e) {
			logger.log(Level.WARNING, "Error loading accounts: " + e.getMessage(), e);
			throw e;
		}*/

	}
	
	/*
	public void deleteAccount(Account account) {
		deleteAccount(account, account.getAccountUUID());
	}
	*/
	
	
	public void deleteAccount(String accountName) {
		Document document = database.getDocument(accountUUID + accountName);
		logger.finest("Deleting account: " + accountName);
		
		try {
			
			if (document.getCurrentRevisionId() != null) {
				document.delete();
			}
		} catch(CouchbaseLiteException cble) {
			logger.log(Level.WARNING, "Error trying to delete account: " + accountName + ", error: " +
					cble.getMessage(), cble);
			cble.printStackTrace();
		}
	}
	
	
	public void deleteAccount(Account account) { //, String uuid) {
		/*
		CryptEngine aesEngine = AESEngine.getInstance();
		
		Map<String, Object> content = new HashMap<>();
		
		content.put("AccountName", account.getName());
		content.put("UserName", account.getUser());
		content.put("Password", 
				new String(encodeBytes(aesEngine.encryptString(encryptionKey, account.getPass()))));
		content.put("OldPassword", 
				new String(encodeBytes(aesEngine.encryptString(encryptionKey, account.getOldPass()))));
		content.put("UpdateTime", System.currentTimeMillis());
		*/
		//Document document = database.getDocument(uuid + account.getName());
		/*
		Document document = database.getDocument(accountUUID + account.getName());
		
		try {
			
			if (document.getCurrentRevisionId() != null) {
				document.delete();
			}
		} catch(CouchbaseLiteException cble) {
			cble.printStackTrace();
		}
		*/
		deleteAccount(account.getName());
	}
	
	/*
	public byte[] decodeString(String toDecode) {	
		
		if (toDecode == null || toDecode.equals("")) {
			//return Base64.getDecoder().decode("");
			return Base64.decode("", Base64.DEFAULT);
		} else {
			//return Base64.getDecoder().decode(toDecode);
			return Base64.decode(toDecode, Base64.DEFAULT);
		}
	}	
	
	
	public byte[] encodeBytes(byte[] toEncode) {
		
		if (toEncode == null) {
			//return Base64.getEncoder().encode(new byte[]{});
			return Base64.encode(new byte[]{}, Base64.DEFAULT);
		} else {
			//return Base64.getEncoder().encode(toEncode);
			return Base64.encode(toEncode, Base64.DEFAULT);
		}
		
	}
	*/

	@Override
	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}
	
	@Override
	public String getEncryptionKey() {
		return this.encryptionKey;
	}
	
	
	public void printConflicts() {
		
		try {
			Query query = database.createAllDocumentsQuery();
			query.setAllDocsMode(Query.AllDocsMode.ONLY_CONFLICTS);
			QueryEnumerator result = query.run();
			logger.info("Running printConflicts");
			
			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
			    QueryRow row = it.next();
			    if (row.getConflictingRevisions().size() > 0) {
			        logger.info("Conflict in document: " + row.getDocumentId());
			    }
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error printing accout conflicts: " + e.getMessage(), e);
			e.printStackTrace();
		}
	}
	
	
	public void purgeDeletes() {
		
		try {
			Query query = database.createAllDocumentsQuery();
			query.setAllDocsMode(Query.AllDocsMode.INCLUDE_DELETED);
			QueryEnumerator result = query.run();
			logger.info("Running purgeDeletes");
			
			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
			    QueryRow row = it.next();
			    Document toCheck = row.getDocument();
			    logger.info("Document: " + toCheck.getId() + ", is _deleted: " + toCheck.isDeleted());
			    
			    if (toCheck.isDeleted()) {
			    		toCheck.purge();
			    		logger.info("Document: " + toCheck.getId() + ", purged");
			    }
			}
			
			logger.info("Done running purgeDeletes");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void saveAccessMap(Collection<AccountAccessMap> values) {
		logger.info("Saving AccessMap");
		final Map saveDoc = new HashMap<>();
		saveDoc.put("docType", ACCESS_MAP_DTYPE);
		saveDoc.put("accounts", values);
		Document mapDoc = database.getDocument("__access_map");
		
		try {
		if (mapDoc.getCurrentRevisionId() != null) {
			mapDoc.update(new Document.DocumentUpdater() {
				
				@Override
				public boolean update(UnsavedRevision arg0) {
					arg0.setUserProperties(saveDoc);
					return true;
				}
			});
		} else {
			mapDoc.putProperties(saveDoc);
		}
		} catch(CouchbaseLiteException e) {
			logger.warning("Error saving AccessMap: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public Collection loadAccessMap() {
		logger.info("Loading AccessMap");
		Query query = database.createAllDocumentsQuery();
		Collection toReturn = null;
		
		try {
			QueryEnumerator enumerator = query.run();
			QueryRow queryRow = null;
			//System.out.println("Count = " +enumerator.getCount());
			
			while ((queryRow = enumerator.next()) != null) {
				Document document = queryRow.getDocument();
				
				if (document.getProperty("docType") != null && document.getProperty("docType").equals(ACCESS_MAP_DTYPE)) {
					logger.fine("Retrieved AccessMap from database");
					toReturn = (Collection) document.getProperty("accounts");
				}
			}
			
		} catch (CouchbaseLiteException e) {
			logger.warning("Error loading AccessMap: " + e.getMessage());
			e.printStackTrace();
		}
		
		//logger.info("Retruning AccessMap of size: " + map.size());
		return toReturn;
	}
	
}
