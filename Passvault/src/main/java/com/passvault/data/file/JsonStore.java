package com.passvault.data.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.passvault.crypto.AESEngine;
import com.passvault.crypto.CryptEngine;
import com.passvault.data.BaseStore;
import com.passvault.data.file.model.AccountModel;
import com.passvault.data.file.model.Database;
import com.passvault.data.file.model.General;
import com.passvault.data.file.model.Settings;
import com.passvault.data.file.model.Store;
import com.passvault.model.syncserver.CheckAccount;
import com.passvault.model.syncserver.SyncRequestFinal;
import com.passvault.model.syncserver.SyncRequestInitial;
import com.passvault.model.syncserver.SyncResponseInitial;
import com.passvault.util.Account;
import com.passvault.util.MRUComparator;
import com.passvault.util.AccountAccessMap;
import com.passvault.util.sync.AccountsChanged;
import com.passvault.util.sync.PassvaultSyncServerReplicationStatus;
import com.passvault.util.sync.ReplicationStatus;


/*
 *  Document structure;;
 *  
 {
	“format”: 1.0,				;;file format type
	“version”: 1,			;;incremented for each modification
	
	“accounts”: [			;;array of all accounts
		{
			“AccountName”: “(string)<name>”,
			“OldPassword”: “(string)”,
			“Password”: “(string)”,
			“URL”: “(string)”,
			“UpdateTime”: “(long)<time in ms>”,
			“UserName”: “(string)”
		}
	],

	“mraMaps”: [			;;holds each accounts mraMap

		{
			“name”: “(string)<name>”,
			“mraTime”: “(long)<time in ms>”,
			“map”: [(int)<hits per day for 35 days>”
		}

	],

	“settings”: {			;;settings 
		“general”: {
				“saveKey”: false,
				“key”: “”,
				“sortMRU”: true,
				“accountUUID”: “”
		},
		“generator”: {
				“overRide”: false,
				“properties”: {
						“allowedCharacter”: [],
						“length”: 32
				}
		},
		“database”: {
				“purge”: false,
				"numberOfDaysBeforePurge": 30
		},
		“sync”: {
				“remote”: {
					“server”: “”,
					“protocol”:  “”,
					“port”: 1,
					“db”: “”,
					“userName”: “”,
					“password”: “”
				},
				“local”: {
					“server”: “”,
					“protocol”:  “”,
					“port”: 1,
					“db”: “”,
					“userName”: “”,
					“password”: “”
				}
		}
	}
}
 */
public class JsonStore extends BaseStore {

	protected String dataFile;
	protected Store dataStore;
	private String key;
	
	//private static JsonStore jsonStore;
	private static Logger logger;
	private final static String DELETED_PASS = "del";
	private final static long DAY_IN_MILLI_SEC = 86_400_000L;
	
	
	static {
		logger = Logger.getLogger("com.passvault.util.data.file");
		//jsonStore = new JsonStore();
	}
	
	
	public JsonStore(String key) {
		this();
		this.key = key;
	}
	
	
	public JsonStore() {
		logger.info("Creating Instance");
		dataFile = System.getProperty("com.passvault.data.file", "data.json");
		logger.fine("Data file set to: " + dataFile);
		dataStore = loadDataStore();
		
		//if store is new setup default values where applicable
		if (dataStore.getAccounts() == null)
			//dataStore.setAccounts(new AccountModel[] {});
			dataStore.setAccounts(new HashMap<String, AccountModel>());
		
		if (dataStore.getMraMaps() == null)
			//dataStore.setMraMaps(new AccountAccessMap[] {});
			dataStore.setMraMaps(new HashMap<String, AccountAccessMap>());
		
		if (dataStore.getSettings() == null) {
			Settings settings = new Settings();
			General general = new General();
			general.setAccountUUID("");
			general.setSaveKey(false);
			general.setSortMRU(true);
			settings.setGeneral(general);
			Database database = new Database();
			database.setPurge(false);
			settings.setDatabase(database);
			dataStore.setSettings(settings);
		}
		
		
		// save datastore
		saveDataStore(null);
	}
	
	
	/*
	 * passing null will use exiting datastore accounts
	 */
	private void saveDataStore(List<Account> accounts, String key) {
		
		/*
		 *  encode all account password - not good since this is alot of extra work. another work around 
		 *  would be to update the Account class to hold both plain text and encoded.
		 */
		
		if ( accounts != null) {
			logger.fine("Saving " + accounts.size() + " accounts");
			CryptEngine aesEngine = AESEngine.getInstance();
			//AccountModel accountsToSave[] = new AccountModel[accounts.size()];
			Map<String, AccountModel> accountsToSave = new HashMap<>();
			int i = 0;
			
			for (Account account : accounts) {
				AccountModel toAdd = new AccountModel();
				toAdd.setAccountName(account.getName());
				toAdd.setUserName(account.getUser());
				toAdd.setUpdateTime(account.getUpdateTime());
				toAdd.setURL(account.getUrl());
				toAdd.setDeleted(account.isDeleted());
				
				/*
				 * once an account is deleted it is no longer loaded with load accounts, so when a List of accounts
				 * is saved the deleted accounts were previously added to the list but their passwords were
				 * not decrypted, so don't encrypt and already encrypted password
				 */
				if (!account.isDeleted()) {
					toAdd.setPassword(new String(encodeBytes(aesEngine.encryptString(key, account.getPass()))));
					toAdd.setOldPassword(new String(encodeBytes(aesEngine.encryptString(key, account.getOldPass()))));
				} else {
					toAdd.setPassword(account.getPass());
					toAdd.setOldPassword(account.getOldPass());
				}
		
				//accountsToSave[i++] = toAdd;
				accountsToSave.put(toAdd.getAccountName(), toAdd);
				logger.finest("Adding account: " + toAdd.getAccountName());
			}
			
			dataStore.setAccounts(accountsToSave);
		}
		
		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	private void saveDataStore(List<Account> accounts) {
		saveDataStore(accounts, key);
	}
	
	
	private void saveDataStore(Account account, String key) {
		logger.fine("Saving account: " + account.getName());
		CryptEngine aesEngine = AESEngine.getInstance();
		
		AccountModel toAdd = new AccountModel();
		toAdd.setAccountName(account.getName());
		toAdd.setUserName(account.getUser());
		toAdd.setUpdateTime(account.getUpdateTime());
		toAdd.setURL(account.getUrl());
		toAdd.setDeleted(account.isDeleted());
		toAdd.setPassword(new String(encodeBytes(aesEngine.encryptString(key, account.getPass()))));
		toAdd.setOldPassword(new String(encodeBytes(aesEngine.encryptString(key, account.getOldPass()))));
		
		Map<String, AccountModel> accounts = dataStore.getAccounts();
		
		// if account exists, update it, or else add it
		boolean accountUpdated = false;
		int i = 0;
		
		if (accounts.containsKey(toAdd.getAccountName())) 
			logger.fine("Updating account");
		else
			logger.fine("Saving new account");
			
		accounts.put(toAdd.getAccountName(), toAdd);

		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	private void saveDataStoreForEncryptedPass(Account account) {
		/*
		 * only used by sync process where accounts received are already encoded/encrypted
		 */
		logger.fine("Saving account: " + account.getName());
		
		AccountModel toAdd = new AccountModel();
		toAdd.setAccountName(account.getName());
		toAdd.setUserName(account.getUser());
		toAdd.setUpdateTime(account.getUpdateTime());
		toAdd.setURL(account.getUrl());
		toAdd.setDeleted(account.isDeleted());
		toAdd.setPassword(account.getPass());
		toAdd.setOldPassword(account.getOldPass());
		
		Map<String, AccountModel> accounts = dataStore.getAccounts();
		
		// if account exists, update it, or else add it
		boolean accountUpdated = false;
		int i = 0;
		
		if (accounts.containsKey(toAdd.getAccountName())) 
			logger.fine("Updating account");
		else
			logger.fine("Saving new account");
			
		accounts.put(toAdd.getAccountName(), toAdd);

		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	
	protected void rotateDataFiles() {

		if (Files.exists(Paths.get(dataFile), new LinkOption[] {})) {
			try {
				logger.finest("Backing up existing data file");
				Files.copy(Paths.get(dataFile), Paths.get(dataFile + "." + dataStore.getVersion() % 10 + ".bak"), 
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				logger.warning("Unable to backup existing data file");
				e1.printStackTrace();
			}
		}
		
	}
	
	
	protected void writeDataFile(boolean updateVersion) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		if (updateVersion)
			dataStore.setVersion(dataStore.getVersion() +  1);
		
		try {
			logger.finest("Saving data file");
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			objectMapper.writeValue(new File(dataFile), dataStore);
		} catch (Exception e) {
			logger.warning("Unable to save data file");
			e.printStackTrace();
		}
	}
	
	
	protected Store loadDataStore() {
		Store toReturn = null;
		
		try {
			if (Files.exists(Paths.get(dataFile), new LinkOption[] {})) {
				byte[] jsonData = Files.readAllBytes(Paths.get(dataFile));
				ObjectMapper objectMapper = new ObjectMapper();
				toReturn = objectMapper.readValue(jsonData, Store.class);
				logger.finest("Using existing store");
			} else {
				toReturn = new Store();
				logger.finest("Creating empty store");
			}
		} catch (Exception e) {
			logger.severe("Unable to load data file: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
		return toReturn;
	}
	
	
	@Override
	public ReplicationStatus syncAccounts(String host, String protocol, int port, String db) {
		return this.syncAccounts(host, protocol, port, db, null);
	}

	@Override
	public ReplicationStatus syncAccounts(String host, String protocol, int port, String db,
			AccountsChanged accountsChanged) {
		
		return this.syncAccounts(host, protocol, port, db, null, null, accountsChanged);
	}

	@Override
	public synchronized ReplicationStatus syncAccounts(String host, String protocol, int port, String path, final String user,
			final String password, final AccountsChanged accountsChanged) {
		
		/*
		 * currently there is no support for syncing anonymously
		 */
		if (user == null || user.equals("")) {
			return new PassvaultSyncServerReplicationStatus("Passvault Sync Server does not support anonymous sync", 
															"Passvault Sync Server does not support anonymous sync",
															false);
		}
		
		logger.info("Starting background sync process");
		final PassvaultSyncServerReplicationStatus replicationStatus = new PassvaultSyncServerReplicationStatus();
		replicationStatus.setRunning(true);
		final PassvaultSyncServerClient client = new PassvaultSyncServerClient(host, protocol, port, path,
				user, password, accountsChanged);
		
		/*
		 * for now keep simple and make it single threaded. making the back end concurrent safe
		 * maybe done later.
		 */
		new Thread(new Runnable() {
			
			@Override
			public void run() {
			
				//AccountModel accountModels[] = dataStore.getAccounts();
				Map<String, AccountModel> accountModels = dataStore.getAccounts();
				
				/*
				 * run sync initial
				 */
				List<CheckAccount> checkAccountList = new ArrayList<>();
				//Map<String, AccountModel> currentAccoountMap = new HashMap<>();
				
				for (AccountModel account : accountModels.values()) {
					checkAccountList.add(new CheckAccount(account.getAccountName(), account.getUpdateTime()));
					//currentAccoountMap.put(account.getAccountName(), account);
				}
			
				SyncRequestInitial syncInitialRequest = new SyncRequestInitial();
				syncInitialRequest.setAccounts(checkAccountList);
				syncInitialRequest.setUser(user);
				syncInitialRequest.setPassword(password);
				SyncResponseInitial initialResponse = client.initialSync(syncInitialRequest);
				List<com.passvault.model.syncserver.Account> accountsSentFromServer;
				List<String> accountsToSendBack;
				long lockTime;
				
				if (initialResponse.getResponseCode() == PassvaultSyncServerClient.Codes.SUCCESS) {
					logger.finest("lock" + initialResponse.getLockTime() + ", code: " + initialResponse.getResponseCode());
					logger.finest("accounts sent back: " + initialResponse.getAccountsToSendBackToClient());
					logger.finest("accounts to send back: " + initialResponse.getSendAccountsToServerList());
					lockTime = initialResponse.getLockTime();
					accountsToSendBack = initialResponse.getSendAccountsToServerList();
					accountsSentFromServer = initialResponse.getAccountsToSendBackToClient();
				} else {
					String error = PassvaultSyncServerClient.Codes.getErrorStringForCode(initialResponse.getResponseCode());
					logger.warning("Error Sending initialSync: " + error);
					replicationStatus.setPullError(error);
					replicationStatus.setPushError(error);
					replicationStatus.setRunning(false);
					
					return;
				}
				//end sync initial
				
				/*
				 * run sync final
				 */
				List<com.passvault.model.syncserver.Account> sendAccounts = new ArrayList<>();
				
				for (String accountName : accountsToSendBack) {
					logger.finest("Adding account to send to server: " + accountName);
					AccountModel account = accountModels.get(accountName);
					com.passvault.model.syncserver.Account toSend = new com.passvault.model.syncserver.Account(
																		account.getAccountName(), 
																		account.getUserName(), 
																		account.getPassword(), 
																		account.getOldPassword(), 
																		account.getURL(), 
																		account.getUpdateTime(), 
																		account.isDeleted());
					sendAccounts.add(toSend);
				}
				
				SyncRequestFinal finalRequest = new SyncRequestFinal();
				finalRequest.setAccounts(sendAccounts);
				finalRequest.setLockTime(lockTime);
				finalRequest.setUser(user);
				finalRequest.setPassword(password);
				
				String success = client.syncFinal(finalRequest);
				
				if (success == null) {
					//success no error returned (bad design)
					logger.info("Accounts Successfully Synced");
					
					/*
					 * for each account sent FROM server add it unless it is marked as deleted and doesnt exist 
					 * on the client, those can be ignored.
					 * 
					 * for any account sent back TO server that was deleted, remove it from JSON store
					 * 
					 * verify update times first in case there were changes during sync
					 */
			
					// merge with accounts sent from server
					for (com.passvault.model.syncserver.Account account : accountsSentFromServer) {
						//if (currentAccoountMap.containsKey(account.getAccountName())) {
						if (accountModels.containsKey(account.getAccountName())) {
							// is not a new account
							//AccountModel current = currentAccoountMap.get(account.getAccountName());
							AccountModel current = accountModels.get(account.getAccountName());
							
							if (account.getUpdateTime() > current.getUpdateTime()) {
								// local account was not changed during sync
								if (account.isDeleted()) {
									//account needs to be removed
									purgeAccount(account.getAccountName());
									logger.fine("Purging account: " + account.getAccountName());
								} else {
									//account needs to be updated
									/*saveAccount(new Account(account.getAccountName(), 
											account.getUserName(), 
											account.getPassword(), 
											account.getOldPassword(), 
											user,							// same as accountUUID 
											account.getUpdateTime(), 
											account.getUrl()));*/
									saveDataStoreForEncryptedPass(new Account(
											account.getAccountName(), 
											account.getUserName(), 
											account.getPassword(), 
											account.getOldPassword(), 
											user,							// same as accountUUID 
											account.getUpdateTime(), 
											account.getUrl()));
								}
							} else {
								logger.fine("Ignoring account: " + account.getAccountName() + ", which has older timestamp");
							}
						} else {
							// new account, save it if it is not marked as deleted
							if (!account.isDeleted()) {
								logger.fine("Saving new account: " + account.getAccountName());
								/*saveAccount(new Account(account.getAccountName(), 
														account.getUserName(), 
														account.getPassword(), 
														account.getOldPassword(), 
														user,							// same as accountUUID 
														account.getUpdateTime(), 
														account.getUrl()));*/
								saveDataStoreForEncryptedPass(new Account(
														account.getAccountName(), 
														account.getUserName(), 
														account.getPassword(), 
														account.getOldPassword(), 
														user,							// same as accountUUID 
														account.getUpdateTime(), 
														account.getUrl()));
							} else {
								logger.fine("Ignoring new account: " + account.getAccountName() + ", that is delete");
							}
						}
					}
					
					// if an account sent to the server was deleted purge from JSON store
					for (String account : accountsToSendBack) {
						//if (currentAccoountMap.get(account).isDeleted()) {
						if (accountModels.get(account).isDeleted()) {
							logger.fine("Purging account: " + account + ", from JSON store");
							purgeAccount(account);
						}
					}
					
					accountsChanged.onAccountsChanged();
					
				} else {
					logger.warning("Sync Failed: " + success);
					replicationStatus.setPullError(success);
					replicationStatus.setPushError(success);
				}
				
				replicationStatus.setRunning(false);
			}
		}, "Passvault Sync Server Thread").start();
		
		return replicationStatus;
	}

	@Override
	public void updateAccountUUID(List<Account> accounts, String uuid) {
		logger.info("Updating account uuid to: " + uuid);
		dataStore.getSettings().getGeneral().setAccountUUID(uuid);
		
		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	@Override
	public void saveAccount(Account account) {
		saveAccount(account, key);
	}

	@Override
	public void saveAccount(Account account, String key) {
		logger.info("Saving account");
		saveDataStore(account, key);
	}

	@Override
	public void saveAccounts(List<Account> accounts) {
		saveAccounts(accounts, key);
	}

	@Override
	public void saveAccounts(List<Account> accounts, String key) {
		logger.info("Saving accounts");
		
		List<Account> accountsToSave = new ArrayList<>();
		
		for (AccountModel account : dataStore.getAccounts().values()) {
			if (account.isDeleted()) {
				logger.finest("Adding delete account: " + account.getAccountName() + ", to save list");
				accountsToSave.add(new Account(account.getAccountName(), 
											  account.getUserName(), 
											  DELETED_PASS, 
											  DELETED_PASS, 
											  dataStore.getSettings().getGeneral().getAccountUUID(), 
											  account.getUpdateTime(), 
											  account.getURL(), 
											  true));
			}
		}
			
		if (accountsToSave.size() > 0) {
			for (Account account : accounts) {
				accountsToSave.add(account);
			}
			
			saveDataStore(accountsToSave, key);
		} else {
			saveDataStore(accounts, key);
		}
	}

	@Override
	public synchronized void loadAccounts(List<Account> accounts) {
		logger.info("Loading accounts");
		CryptEngine aesEngine = AESEngine.getInstance();
		
		for (AccountModel account : dataStore.getAccounts().values()) {
			
			if (account.isDeleted()) 
				continue;
			
			Account toAdd = null;
			String password = null;
			String oldPassword = null;
			boolean decrypted = true;
			
			try {
				password = aesEngine.decryptBytes(key, decodeString(account.getPassword()));
				oldPassword = aesEngine.decryptBytes(key, decodeString(account.getOldPassword()));
			} catch (Exception e) {

				if (password == null) {
					password = account.getPassword();
					logger.warning("Unable to decrypt current password for account: " + account.getAccountName());
					decrypted = false;
					
					try {
						oldPassword = aesEngine.decryptBytes(key, decodeString(account.getOldPassword()));
					} catch (Exception e1) {
						oldPassword = account.getOldPassword();
					}
				} else {
					// if just old pass cant be decrypted to mark account
					oldPassword = password;
					logger.warning("Unable to decrypt current password for account: " + account.getAccountName() +
							", setting password to the current password.");
				}
					
				logger.warning("Unable to decrypt password for: " + account.getAccountName() + ", " + e.getMessage());
				e.printStackTrace();
			}
			
			toAdd = new Account(account.getAccountName(), 
							account.getUserName(), 
							password, 
							oldPassword,
							dataStore.getSettings().getGeneral().getAccountUUID(), 
							account.getUpdateTime(), 
							account.getURL(),
							account.isDeleted());

			if (!decrypted)
				toAdd.setValidEncryption(false);
			
			if (!accounts.contains(toAdd))
				accounts.add(toAdd);
		}
		
	}

	@Override
	/*
	 * Account will not be deleted from the datastore, rather it will just be marked as deleted.
	 */
	public synchronized void deleteAccount(String accountName) {
		logger.info("Deleting Account: " + accountName);
		
		for (AccountModel accountModel : dataStore.getAccounts().values()) {
			if (accountModel.getAccountName().equalsIgnoreCase(accountName)) {
				logger.finest("Marking account: " + accountModel.getAccountName() + ", as deleted");
				accountModel.setDeleted(true);
				accountModel.setUpdateTime(System.currentTimeMillis());
				break;
			}
		}
//TODO - Access Maps should be moved into each individual account
		dataStore.getMraMaps().remove(accountName);
		
		rotateDataFiles();
		writeDataFile(true);
	}

	@Override
	public void deleteAccount(Account account) {
		deleteAccount(account.getName());
	}

	@Override
	public void setEncryptionKey(String encryptionKey) {
		this.key = encryptionKey;
	}
	
	@Override
	public String getEncryptionKey() {
		return this.key;
	}

	@Override
	public void saveAccessMap(Collection<AccountAccessMap> values) {
		Map<String, AccountAccessMap> mraMaps = dataStore.getMraMaps();
		
		for (AccountAccessMap accountAccessMap : values) {
			mraMaps.put(accountAccessMap.getName(), accountAccessMap);
		}
		
		logger.finest("Saving access map");
		writeDataFile(false);
	}

	@Override
	public Collection loadAccessMap() {
		logger.finest("Loading access map");
		return dataStore.getMraMaps().values(); 
	}

	
	@Override
	public void purgeDeletes() {
		logger.info("Running purge deletes");
		
		if (!dataStore.getSettings().getDatabase().isPurge()) {
			logger.info("Purge is not set, doing nothing");
			return;
		}
		
		long numberOfMilliToKeepDeletes = 
				dataStore.getSettings().getDatabase().getNumberOfDaysBeforePurge() * DAY_IN_MILLI_SEC;
		List<AccountModel> accountsToRemove = new ArrayList<>();
		long currentTime = System.currentTimeMillis();
		
		for (AccountModel account : dataStore.getAccounts().values()) {
			if (account.isDeleted() && (currentTime - account.getUpdateTime()) > numberOfMilliToKeepDeletes) {
				logger.info("Purging deleted account: " + account.getAccountName());
				accountsToRemove.add(account);
				continue;
			}
			
			logger.finest("Keeping account: " + account.getAccountName());
		}
		
		for (AccountModel accountModel : accountsToRemove) {
			dataStore.getAccounts().remove(accountModel.getAccountName());
		}
		
		// for any dangling AccountAccessMaps, remove
		Map<String, AccountModel> accountModels = dataStore.getAccounts();
		List<String> acmToRemove = new ArrayList<>();
		for (AccountAccessMap acm : dataStore.getMraMaps().values()) {
			if (!accountModels.containsKey(acm.getName()) || accountModels.get(acm.getName()).isDeleted()) {
				logger.finest("Removing AccountAccessMap for: " + acm.getName());
				acmToRemove.add(acm.getName());
			} 
		}
		
		for (String accountName : acmToRemove) {
			dataStore.getMraMaps().remove(accountName);
		}

		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	@Override
	public void saveSettings(Settings settings) {
		dataStore.setSettings(settings);
		writeDataFile(false);
	}


	@Override
	public Settings loadSettings() {
		return dataStore.getSettings();
	}


	private void purgeAccount(String accountName) {
		//List<AccountModel> accountsToKeep = new ArrayList<>();
		long currentTime = System.currentTimeMillis();
		
		dataStore.getAccounts().remove(accountName);
		dataStore.getMraMaps().remove(accountName);
		
		rotateDataFiles();
		writeDataFile(true);
	}
	
	
	// this will be used to run any updates depeneding on the format of the database
	public void checkForUpdate() {
		logger.info("Checking for updates");
		double currentFormat = dataStore.getFormat();
		
		if (currentFormat == 1.0) {
			// run updates for encryption/decryption to use IV
			logger.info("Running updates from 1.0 format");
			AESEngine engine = (AESEngine) AESEngine.getInstance();
			Map<String, AccountModel> accountsMap = dataStore.getAccounts();
			Iterator<String> it = accountsMap.keySet().iterator();
			
			while (it.hasNext()) {
				AccountModel account = accountsMap.get(it.next());
				
				if (account.isDeleted())
					continue;
				
				byte[] encPassBytesV1 = decodeString(account.getPassword());
				
				try {
					logger.info("Updating password for account: " + account.getAccountName());
					String decPass = engine.decryptBytesDeprecated(key, encPassBytesV1);
					byte[] encPassBytesV2 = engine.encryptString(key, decPass);
					String newPass = new String(encodeBytes(encPassBytesV2));
					account.setPassword(newPass);
				} catch (Exception e) {
					logger.severe("Unable to update password for account: " + account.getAccountName());
					e.printStackTrace();
					continue;
				}
				
				byte[] encOldPassBytesV1 = decodeString(account.getOldPassword());
				
				try {
					logger.info("Updating old password for account: " + account.getAccountName());
					String decOldPass = engine.decryptBytesDeprecated(key, encOldPassBytesV1);
					byte[] encOldPassBytesV2 = engine.encryptString(key, decOldPass);
					String newOldPass = new String(encodeBytes(encOldPassBytesV2));
					account.setOldPassword(newOldPass);
				} catch (Exception e) {
					logger.severe("Unable to update oldpassword for account: " + account.getAccountName());
					e.printStackTrace();
					// just set to current
					account.setOldPassword(account.getPassword());
				}
				
			}
			
			/*
			 *  just assume all went good, BAD - can get into all kinds of situations where some updated/ some not
			 *  and would take way more work then I wont to do for now
			 */
			dataStore.setFormat(1.1);
			writeDataFile(true);
			
		}
	}

}
