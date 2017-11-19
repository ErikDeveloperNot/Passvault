package com.passvault.tools;

import java.util.ArrayList;
import java.util.List;

import com.passvault.crypto.AESEngine;
import com.passvault.util.Account;
import com.passvault.util.Utils;
import com.passvault.util.data.Store;
import com.passvault.util.data.couchbase.CBLStore;

public class Upgrade {

	private static String encryptionKey;
	private static String dataFile;
	
	static Store cblStore = null;
	
	public static void main(String[] args) {
		// TODO options to run specific upgrades
		dataFile = args[0];
		upgradeToCBL();

	}
	
	
	private static void upgradeKey() {
		char[] key = System.console().readPassword("Enter encrypt/decrypt key:");
		
	}
	
	private static void upgradeToCBL() {
		
		char[] key = System.console().readPassword("Enter encrypt/decrypt key:");
		
		/*
		StringBuilder finalKey = new StringBuilder();
		
		if (key.length%PasswordVault.KEY_SIZE_FACTOR != 0) {
			char[] padding = new char[PasswordVault.KEY_SIZE_FACTOR-(key.length%PasswordVault.KEY_SIZE_FACTOR)];
			
			for (int i=0; i<padding.length; i++) {
				padding[i] = '0';
			}
			
			encryptionKey = finalKey.append(key).append(padding).toString();
		} else {
			encryptionKey = finalKey.append(key).toString();
		}
		*/
		
		try {
			encryptionKey = AESEngine.finalizeKey(new String(key), PasswordVault.KEY_SIZE_FACTOR);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		List<Account> accounts = new ArrayList<>();
		Utils.loadAccounts(dataFile, encryptionKey, accounts);
		
		
		
	p("key length=" + encryptionKey.length());	
		
		try {
			cblStore = new CBLStore("pass_vault", 
					CBLStore.DatabaseFormat.SQLite, encryptionKey);
System.out.println(accounts.size());		
			//cblStore.loadAccounts(accounts);
			cblStore.saveAccounts(accounts);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//startCmdLineLoop(accounts);
		//new PasswordVault(finalKey.toString(), dataFile);
	}
	
/*	
	private static void startCmdLineLoop(List<Account> accounts) {
		Cmd.showAccounts();
		//Cmd.showMenu();
		
		boolean running = true;
		
		while (running) {
			Cmd.showMenu();
			String selection = System.console().readLine("Enter Selection: ");
			
			switch (selection) {
			case "1":
				Cmd.getAccountPassword(accounts);
				break;
			case "2":
				//Cmd.createAccount(accounts, file, key);
				createAccount(accounts, encryptionKey);
				break;
			case "3":
				//Cmd.updateAccount(accounts, file, key);
				break;
			case "4":
				//Cmd.deleteAccount(accounts, file, key);
				break;
			case "5":
				Cmd.showAccounts(accounts);
				//Cmd.showMenu();
				break;
			case "6":
				//key = Cmd.changeKey(accounts, file);
				break;
			case "7":
				running = false;
				break;
			default:
				p("Invalid Selection, enter [1-6]");
				continue;
			}
			
		}
	}
*/	
	

	
	private static void p(String print) {
		System.out.println(print);
	}

}
