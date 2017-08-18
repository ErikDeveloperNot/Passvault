package com.passvault.ui.text;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.passvault.crypto.AESEngine;
import com.passvault.tools.PasswordVault;
import com.passvault.util.Account;
import com.passvault.util.RandomPasswordGenerator;
import com.passvault.util.Utils;
import com.passvault.util.couchbase.AccountsChanged;
import com.passvault.util.couchbase.CBLStore;

public class Cmd {
	
	private static final String DEFAULT_PASSWORD_GENERATOR = "com.passvault.util.DefaultRandomPasswordGenerator";
	
	private List<Account> accounts;
	private String key;
	private String dataFile;
	private CBLStore cblStore;
	private StoreType storeType;
	private static Logger logger;
	
	public enum StoreType {CBL_STORE, DAT_FILE};
	
	static {
		logger = Logger.getLogger("com.passvault.ui.text");
	}
	
	
	public Cmd(List<Account> accounts, String key, String dataFile) {
		logger.finest("Creating Cmd with data file: " + dataFile);
		this.key = key;
		this.accounts = accounts;
		this.dataFile = dataFile;
		storeType = StoreType.DAT_FILE;
	}
	
	
	public Cmd(List<Account> accounts, String key, CBLStore cblStore) {
		logger.finest("Creating Cmd with couchbase lite");
		this.key = key;
		this.accounts = accounts;
		this.cblStore = cblStore;
		storeType = StoreType.CBL_STORE;
	}
	
	
	public void showAccounts() {
		p("");
		p("Accounts:");
		int i=1;
		
		synchronized (accounts) {
			for (Account account : accounts) {
				p((i++) + ". " + account.getName());
			}
		}
		
		p("");
		
	}
	
	
	public void showMenu() {
		p("");
		p("1. Get Account Password");
		p("2. Create Account");
		p("3. Update Account");
		p("4. Delete Account");
		p("5. Show Accounts");
		p("6. Change Encrypt/Decrypt Key");
		p("7. Sync Accounts");
		p("8. Exit");
		p("");
	}
	
	
	public void loadAccounts() {
		switch (storeType) {
		case CBL_STORE:
			try {
				cblStore.printConflicts();
				
				if (System.getProperty("com.passvault.store.purge", "false").equalsIgnoreCase("true"))
					cblStore.purgeDeletes();
				
				cblStore.loadAccounts(accounts);
			} catch (Exception e) {
				logger.severe("Error loading accounts from couchbase lite: " + e.getMessage());
				e.printStackTrace();
			};
			break;
		case DAT_FILE:
			Utils.loadAccounts(dataFile, key, accounts);
			break;
		default:
			Utils.loadAccounts(dataFile, key, accounts);
			break;
		}
	}
	
	
	public void syncAccounts() {
		
		if (cblStore == null) {
			p("\nCan only Sync accounts with cbl storage type.");
			return;
		}
		
		AccountsChanged accountsChngImpl = new AccountsChanged() {
			
			@Override
			public void onAccountsChanged() {
				logger.finest("Accounts updated, reloading");
				
				synchronized (accounts) {
					try {
						accounts.clear();
						cblStore.loadAccounts(accounts);
					} catch (Exception e) {
						logger.warning("Error reloading accounts : " + e.getMessage());
						e.printStackTrace();
					};
				}
				
				showAccounts();
				showMenu();
				p("\nEnter Selection: ");
			}
		};
		
		/*
		cblStore.syncAccounts("10.112.151.106", "http", SyncGatewayClient.DEFAULT_PORT, 
				SyncGatewayClient.DEFAULT_BUCKET, accountsChngImpl);
		*/
		
		new SyncCmd(cblStore, accountsChngImpl, accounts);
		
	}
	
	
	public void getAccountPassword() {
		String toUpdate = System.console().readLine("Enter Account Number to copy password to the clipboard: ");
		int x = -1;
		
		try {
			x = Integer.parseInt(toUpdate);
		} catch (NumberFormatException e) {
			p("Enter a valid digit corresponding to an Account.");
			return;
		}
		
		Account getAccount = null;
		
		try {
			getAccount = accounts.get(x-1);
		} catch (Exception e) {
			p("No Account is mapped to " + (x));
			return;
		}
		
		p("1. Get current password");
		p("2. Get old password");
		String whichPass = System.console().readLine("Enter [1] for current password, [2] for old password: ");
		StringSelection stringSelection = null;
		
		if (whichPass.equalsIgnoreCase("2"))
			stringSelection = new StringSelection(getAccount.getOldPass());
		else
			stringSelection = new StringSelection(getAccount.getPass());
		
		Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipBoard.setContents(stringSelection, stringSelection);
		p("Password copied to clipboard for account " + getAccount.getName() + 
				", for username " + getAccount.getUser() + "\n");
		
		if (!getAccount.getUrl().equalsIgnoreCase("http://")) {
			p("Account URL: " + getAccount.getUrl());
			String open = System.console().readLine("Open browser to account URL [Y/N]: ");
			
			if (open.equalsIgnoreCase("Y")) 
				launchBrowser(getAccount.getUrl());
		}
			
	}
	
	
	public void createAccount() {
		String name = System.console().readLine("Enter Account Name: ");
		String user = System.console().readLine("Enter Account User Name: ");
		String url = System.console().readLine("Enter site URL or just press <return>: ");
		String pass = getPassword();
		Account newAccount = null;
		
		if (name.length()>0 && user.length()>0) {	
			newAccount = new Account(name, user,pass, pass, "", System.currentTimeMillis(), url);
			
			if (accounts.contains(newAccount)) {
				System.err.println("\nAn Account with the same name already exists\n");
				return;
			}
			
			
			accounts.add(newAccount);
		} else {
			p("Enter valid values Account name, and User name/");
			return;
		}
		
		switch (storeType) {
		case CBL_STORE:
			cblStore.saveAccount(newAccount);
			break;
		case DAT_FILE:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		default:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		}

		p("Account Saved");
	}
	
	
	public void updateAccount() {
		String toUpdate = System.console().readLine("Enter Account Number to Update: ");
		int x = -1;
		
		try {
			x = Integer.parseInt(toUpdate);
		} catch (NumberFormatException e) {
			p("Enter a valid digit corresponding to an Account.");
			return;
		}
		
		Account updateAccount = null;
		
		try {
			updateAccount = accounts.get(x-1);
		} catch (Exception e) {
			p("No Account is mapped to " + (x));
			return;
		}
		
		String user = System.console().readLine("Enter Account User Name or Press <Enter> to keep " +
				" current User Name [" + updateAccount.getUser() + "]: ");
		String url = System.console().readLine("Enter URL or Press <Enter> to keep " +
				" current URL [" + updateAccount.getUrl() + "]: ");
		String pass = getPassword();
		
		if (user.length() > 0)
			updateAccount.setUser(user);
		
		if (url.length() > 0)
			updateAccount.setUrl(url);
		
		if (!pass.equals(updateAccount.getOldPass()))
			updateAccount.setOldPass(updateAccount.getPass());
		
		updateAccount.setPass(pass);
		updateAccount.setUpdateTime(System.currentTimeMillis());
		
		switch (storeType) {
		case CBL_STORE:
			cblStore.saveAccount(updateAccount);
			break;
		case DAT_FILE:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		default:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		}
		
		p("Account Updated");
	}
	
	
	public String getPassword() {
		String password = null;
		
		String selection = System.console().readLine("Use generated password [y/n]: ");
		
		if (!selection.equalsIgnoreCase("n")) {
			password = generatePassword();
		} else {
			password = new String(System.console().readPassword("Enter Account Password: "));
			/*
			if (password.length() == 0)
				password = Account.BLANK_PASSWORD;
			*/
		}
		
		return password;
	}
	
	
	private String generatePassword() {
		//String password = null;
		//String generator = System.getProperty("com.passvault.generator", DEFAULT_PASSWORD_GENERATOR);
		RandomPasswordGenerator passwordGenerator = null;
		
		String useDefaults = System.console().readLine("Use default password contraints [y/n]: ");
		
		if  (useDefaults.equalsIgnoreCase("n")) {
			boolean lower = false, upper = false, digits = false, special = false;
			int length = 32;
			String useLower = System.console().readLine("Allow lowercase letters [y/n]: ");
			String useUpper = System.console().readLine("Allow uppercase letters [y/n]: ");
			String useDigits = System.console().readLine("Allow digits [y/n]: : ");
			String useSpecial = System.console().readLine("Allow special characters [y/n]: ");
			String passLength = System.console().readLine("Enter password length: ");
			
			try {
				length = Integer.parseInt(passLength);
			} catch (NumberFormatException e) {
				p("Invalid password length, using 32");
			}
			
			lower = useLower.equalsIgnoreCase("n") ? false : true;
			upper = useUpper.equalsIgnoreCase("n") ? false : true;
			digits = useDigits.equalsIgnoreCase("n") ? false : true;
			special = useSpecial.equalsIgnoreCase("n") ? false : true;
			
			try {
				Class<?> clazz = Class.forName(System.getProperty(
						"com.passvault.generator", DEFAULT_PASSWORD_GENERATOR));
				Constructor<?> cons = clazz.getConstructor(Integer.class, Boolean.class, Boolean.class,
						Boolean.class, Boolean.class);
				passwordGenerator = (RandomPasswordGenerator)cons.newInstance(length, lower, upper,
						special, digits);
			} catch (Exception e) {
				logger.severe("Error loading password generator class: " + e.getMessage());
				e.printStackTrace();
			}
			
			boolean loop = true;
			
			while (loop) {
				p("");
				p("1. View allowed characters");
				p("2. Remove allowed character");
				p("3. Add allowed character");
				p("4. Generate password");
				String choice = System.console().readLine("Enter choice[1, 2, 3, 4], default[4]: ");
				
				switch (choice) {
				case "1":
					List<Character> characters = passwordGenerator.getAllowedCharactres();
					p(characters.toString());
					break;
				case "2":
					String remove = System.console().readLine("Enter character(s) to remove " +
							"seaparated by spaces: ");
					StringTokenizer toker = new StringTokenizer(remove, " ");
					
					while (toker.hasMoreTokens()) {
						String next = toker.nextToken();
						
						if (next.trim().length() > 1)
							continue;
						
						passwordGenerator.removedAllowedCharacters(next.charAt(0));
					}
					break;
				case "3":
					String add = System.console().readLine("Enter character(s) to remove " +
							"seaparated by spaces: ");
					StringTokenizer toker2 = new StringTokenizer(add, " ");
					
					while (toker2.hasMoreTokens()) {
						String next = toker2.nextToken();
						
						if (next.trim().length() > 1)
							continue;
						
						passwordGenerator.setAllowedCharacters(next.charAt(0));
					}
					break;
				default:
					loop = false;
					break;
				}
			}
			
		} else {
			try {
				Class<?> clazz = Class.forName(System.getProperty(
						"com.passvault.generator", DEFAULT_PASSWORD_GENERATOR));
				Constructor<?> cons = clazz.getConstructor();
				passwordGenerator = (RandomPasswordGenerator)cons.newInstance();
			} catch (Exception e) {
				logger.severe("Error loading password generator class: " + e.getMessage());
				e.printStackTrace();
			}
		}

		return passwordGenerator.generatePassword();
	}
	
	
	public void deleteAccount() {
		Account removeAccount = null;
		String toDelete = System.console().readLine("Enter Account Number to Delete: ");
		int x = -1;
		
		try {
			x = Integer.parseInt(toDelete);
		} catch (NumberFormatException e) {
			p("Enter a valid digit corresponding to an Account.");
			return;
		}
		
		try {
			removeAccount = accounts.remove(x-1);
		} catch (Exception e) {
			p("No Account is mapped to " + (x));
			return;
		}
		
		removeAccount.setUpdateTime(System.currentTimeMillis());
		
		switch (storeType) {
		case CBL_STORE:
			cblStore.deleteAccount(removeAccount);
			break;
		case DAT_FILE:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		default:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		}
		
		p("Account Removed");
		
	}
	
	
	private void launchBrowser(String url) {
		
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			
			if (desktop.isSupported(Action.BROWSE)) {
				try {
					desktop.browse(new URI(url));
				} catch (Exception e) {
					p("Error trying to open page: " + e.getMessage());
					logger.warning("Error opening browser to: " + url + "\n" + e.getMessage());
					e.printStackTrace();
				}
			} else {
				p("Browser open action not supported");
			}
		} else {
			p("Browser launch not supported on this platform");
		}
	}
	
	
	public String changeKey() {
		char[] keyChars = System.console().readPassword("Enter encrypt/decrypt key:");
		
		try {
			this.key = AESEngine.finalizeKey(new String(keyChars), PasswordVault.KEY_SIZE_FACTOR);
		} catch (Exception e) {
			p("Failed to Change Encryption Key: " + e.getMessage());
			logger.warning("Failed to Change Encryption Key: " + e.getMessage());
			e.printStackTrace();
			return key;
		}
		
		/*
		StringBuilder finalKey = new StringBuilder();
		
		if (keyChars.length%PasswordVault.KEY_SIZE_FACTOR != 0) {
			char[] padding = new char[PasswordVault.KEY_SIZE_FACTOR-(keyChars.length%PasswordVault.KEY_SIZE_FACTOR)];
			
			for (int i=0; i<padding.length; i++) {
				padding[i] = '0';
			}
			
			finalKey.append(keyChars).append(padding);
		} else {
			finalKey.append(keyChars);
		}
		
		String key = finalKey.toString();
		this.key = key;
		*/
		
		switch (storeType) {
		case CBL_STORE:
			cblStore.setEncryptionKey(key);
			cblStore.saveAccounts(accounts);
			break;
		case DAT_FILE:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		default:
			Utils.saveAccounts(dataFile, key, accounts);
			break;
		}
		
		p("Key Updated");
		return key;
	}
	
	
	static void p(String print) {
		System.out.println(print);
	}
	
	
	
	public static void showUse() {
		p("passvault [file or db name] [cbl | file]\n");
		p("By default data file will be in working directory named password_vault.dat");
	}
	
}
