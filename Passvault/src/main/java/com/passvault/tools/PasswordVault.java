package com.passvault.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.passvault.crypto.AESEngine;
import com.passvault.ui.text.Cmd;
import com.passvault.util.Account;
import com.passvault.util.couchbase.CBLStore;

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
		logger.info("Starting Passvault");
		//accounts = new ArrayList<>();
		accounts = Collections.synchronizedList(new ArrayList<Account>());
		//this.file = file;
		//this.key = key;
		
		if (storeType.equalsIgnoreCase("cbl")) {
			CBLStore cblStore = new CBLStore(file, CBLStore.DatabaseFormat.SQLite, key);
			logger.fine("Using Couchbase Lite for persistence");
			cmd = new Cmd(accounts, key, cblStore);
		} else {
			logger.fine("Using data file for persistence");
			cmd = new Cmd(accounts, key, file);
		}
		
		loadAccounts();

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
					new PasswordVault(AESEngine.finalizeKey(unPaddedKey, KEY_SIZE_FACTOR), "cbl", args[0]);
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
