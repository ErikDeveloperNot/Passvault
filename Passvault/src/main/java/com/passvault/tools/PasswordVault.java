package com.passvault.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.passvault.crypto.AESEngine;
import com.passvault.ui.text.Cmd;
import com.passvault.util.Account;
import com.passvault.util.data.Store;
import com.passvault.util.data.couchbase.CBLStore;
import com.passvault.util.data.file.JsonStore;

public class PasswordVault {
	
	public static final int KEY_SIZE_FACTOR = 32;
	public static final String DEFAULT_FILE = ".password_vault.dat";
	private List<Account> accounts;
	private Cmd cmd;
	//private String file;
	//private String key;
	
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.tools");
	}
	
	public PasswordVault(String key) {
		this(key, "file", DEFAULT_FILE);
	}

	
	public PasswordVault(String key, String storeType, String file) {
/*
String oldKey = null;
String newKey = null;
CBLStore store = null;
try {
	newKey = AESEngine.finalizeKey(key, KEY_SIZE_FACTOR);
	key = AESEngine.finalizeKeyOld(key, KEY_SIZE_FACTOR);
	
} catch (Exception e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
*/
		logger.info("Starting Passvault");
		//accounts = new ArrayList<>();
		accounts = Collections.synchronizedList(new ArrayList<Account>());
		//this.file = file;
		//this.key = key;
		
		if (storeType.equalsIgnoreCase("cbl")) {
			Store cblStore = new CBLStore(file, CBLStore.DatabaseFormat.SQLite, key);
//store = cblStore;
			logger.fine("Using Couchbase Lite for persistence");
			cmd = new Cmd(accounts, key, cblStore);
		} else if (storeType.equalsIgnoreCase("json")) {
			Store jsonStore = new JsonStore();
			jsonStore.setEncryptionKey(key);
			logger.fine("Using Json Store for persistence");
			cmd = new Cmd(accounts, key, jsonStore);
		} else {
			logger.fine("Using data file for persistence");
			cmd = new Cmd(accounts, key, file);
		}
		
		loadAccounts();
//store.saveAccounts(accounts, newKey);

/*	
//used when changing key size from 16 to 32	
try {
	String newKey = AESEngine.finalizeKey(key, 32);
	Utils.saveAccounts(file, newKey, accounts);
} catch (Exception e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
*/

		startCmdLineLoop();
		
		// TODO - clean up one time replication - need to see if I need to do a call
		System.exit(0);
	}
	
	
	private void startCmdLineLoop() {
		cmd.showAccounts();
		//Cmd.showMenu();
		
		boolean running = true;
		
		while (running) {
			cmd.showMenu();
			String selection = System.console().readLine("Enter Selection: ");
			
			switch (selection) {
			case "1":
				cmd.getAccountPassword();
				break;
			case "2":
				cmd.createAccount();
				break;
			case "3":
				cmd.updateAccount();
				break;
			case "4":
				cmd.deleteAccount();
				break;
			case "5":
				cmd.showAccounts();
				//Cmd.showMenu();
				break;
			case "6":
				//key = cmd.changeKey();
				cmd.changeKey();
				break;
			case "7":
				cmd.syncAccounts();
				break;
			case "8":
				running = false;
				cmd.shutDown();
				break;
			default:
				p("Invalid Selection, enter [1-8]");
				continue;
			}
			
		}
	}
	
	
	private void loadAccounts() {
		logger.finest("Loading accounts");
		cmd.loadAccounts();
	}
	
	
	
	private static void p(String line) {
		System.out.println(line);
	}
	
	/*
	 * entry point for command line version
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length > 0 && args[0].contains("--help")) {
			Cmd.showUse();
			System.exit(1);
		}
		
		//get user key, make sure length is multiple of 16
		char[] key = System.console().readPassword("Enter encrypt/decrypt key:");
		String unPaddedKey = new String(key);
		
		if (args.length > 0) {
			
			if (args.length >= 2) {
				String storeType = args[1];
//userName=args[2];				
				if (storeType.equalsIgnoreCase("cbl")) {
//new PasswordVault(unPaddedKey, "cbl", args[0]);
					new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR), "cbl", args[0]);
				} else if (storeType.equalsIgnoreCase("json")) {
					new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR), "json", args[0]);
				} else {
					new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR), "file", args[0]);
				}
				
			} else {
				new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR), "file", args[0]);
			}
		} else {
			new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR));
		}
	}

}
