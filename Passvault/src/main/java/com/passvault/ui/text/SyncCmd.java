package com.passvault.ui.text;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.EmailValidator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.passvault.crypto.AESEngine;
import com.passvault.util.Account;
import com.passvault.util.Utils;
import com.passvault.util.couchbase.AccountsChanged;
import com.passvault.util.couchbase.CBLStore;
import com.passvault.util.couchbase.SyncGatewayClient;
import com.passvault.util.model.Gateway;
import com.passvault.util.model.Gateways;
import com.passvault.util.register.RegisterAccount;
import com.passvault.util.register.RegisterResponse;

public class SyncCmd {

	private AccountsChanged accountsChanged;
	private CBLStore cblStore;
	private List<Account> accounts;
	private static String key;
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.ui.text");
		
		try {
			key = AESEngine.finalizeKey("hSDFnv_", AESEngine.KEY_LENGTH_256);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public SyncCmd(CBLStore cblStore, AccountsChanged accountsChanged, List<Account> accounts) {
		this.accountsChanged = accountsChanged;
		this.cblStore = cblStore;
		this.accounts = accounts;
		run();
	}
	
	
	private void run() {
		
		Cmd.p("1. Sync with free service");
		Cmd.p("2. Sync with personal service");
		Cmd.p("3. Back to main menu");
		Cmd.p("");
		String choice = System.console().readLine("Enter Choice: ");
		
		switch (choice) {
		case "1":
			//syncWithRemoteV2();
			syncWithRemoreV1();
			break;
		case "2":
			syncWithLocal();
			break;
		case "3":
			return;
		default:
			Cmd.p("Invalid Choice");
			run();
			break;
		}
	}
	
	
	private void syncWithLocal() {
		Gateway[] gateways = null;
		
		try {
			gateways = loadSyncConfig("local");
		} catch (Exception e) {
			System.err.println("\nError Loading Sync Gateway Config: " + e.getMessage());
			logger.log(Level.WARNING, "Error Loading Sync Gateway Config: " + e.getMessage(), e);
			//e.printStackTrace();
			Cmd.p("\nCould not load config, create new config");
			createGatewayConfig();
			return;
		}
		
		if (gateways == null || gateways.length == 0) {
			String choice = System.console().readLine("\nNo gateway configurations found, enter [c/C] to create a new" +
					" gateway configuration or any other key to return");
			
			if (choice.equalsIgnoreCase("c")) {
				Gateway newConfig = createGatewayConfig();
				
				if (newConfig != null)
					sync(newConfig);
				
				return;
			}
			
			//syncWithLocal();
		} else {
			Cmd.p("\nExisting Configurations:");
			
			for (int i=0; i<gateways.length; i++) 
				Cmd.p(i+1 + ": " + gateways[i].getProtocol() + "://" + gateways[i].getServer() +
						"/" + gateways[i].getBucket());
			
			Cmd.p("\n1. Enter gateway number to sync with");
			Cmd.p("2. Enter [C/c] to create a new Gateway definition");
			Cmd.p("3. Enter [D/d] to delete an existing Gateway definition");
			Cmd.p("");
			String choice = System.console().readLine("Enter Choice: ");
			
			if (choice.equalsIgnoreCase("c")) {
				Gateway newConfig = createGatewayConfig();
				
				if (newConfig != null)
					sync(newConfig);
				
				return;
			}
			
			if (choice.equalsIgnoreCase("d")) {
				String delete = System.console().readLine("Enter gateway number to delete: ");
				int toDelete = verifyChoice(delete, gateways.length);
			
				if (toDelete == -1) {
					Cmd.p("\nInvalid Entry: " + delete);
					syncWithLocal();
					return;
				}
				
				deleteGatewayConfig(gateways[toDelete], gateways);
				syncWithLocal();
				return;
			}
				
			int config = verifyChoice(choice, gateways.length);
			
			if (config == -1) {
				Cmd.p("\nInvalid entry...");
				syncWithLocal();
				return;
			}
			
			sync(gateways[config]);
		}
	}
	
	
	private void syncWithRemoreV1() {
		Gateway[] gateways = null;
		
		try {
			gateways = loadSyncConfig("remote");
		} catch (Exception e) {
			System.err.println("\nError Loading Sync Gateway Config: " + e.getMessage());
			logger.log(Level.WARNING, "Error Loading Sync Gateway Config: " + e.getMessage(), e);
		}
		
		if (gateways == null || gateways.length == 0) {
			Cmd.p("\nIn order to Sync with the free service you need to register with your email address\n" +
					"and set a password.\n");
			Cmd.p("\n1. Enter your email/password to create a new sync account.");
			Cmd.p("2. Enter your email/password to use an existing sync account.");
			String choice = System.console().readLine("Enter Choice: ").trim();
			
			if (choice == null || (!choice.equalsIgnoreCase("1") && !choice.equalsIgnoreCase("2"))) {
				Cmd.p("Invalid entry, should be 1 or 2.");
				return;
			}
			
			String email = System.console().readLine("\nEnter your Email: ");
			char[] pass = System.console().readPassword("Enter Password: ");
			
			if (pass == null || pass.length == 0) {
				System.out.println("Password can't be blank");
				return;
			}
			
			RegisterAccount registerAccount = new RegisterAccount();
			RegisterResponse regResp;
			
			
			if (choice.equalsIgnoreCase("1")) {
				logger.info("Sending new registration request for email: " + email);
				regResp = registerAccount.registerV1(email, new String(pass));
			} else {
				regResp = registerAccount.getGatewatConfig();
				logger.info("Retrieving remote gateway configuration");
			}
			
	        if (regResp.success()) {
	        		Gateway remoteGateway = (Gateway)regResp.getReturnValue();
	        		logger.info("Received successful  response " + 
	        				(remoteGateway != null ? "with gateway config" : "with no gateway config"));
		    		
	        		if (remoteGateway != null) {
	        			
	        			if (choice.equalsIgnoreCase("2")) {
	        				// update dummy usr/password from sync config
	        				remoteGateway.setUserName(email);
	        				remoteGateway.setPassword(new String(pass));
	        			}
	        			
	        			Cmd.p(regResp.getMessage());
	        			logger.info("Saving gateway config");
	        			saveRemoteGateway(remoteGateway);
	        			// use email instead of a UUID for v1
	        			logger.fine("Updating AccountUUID");
	        			cblStore.updateAccountUUID(accounts, email);
	        		} else {
	        			Cmd.p("Error: Failed to retrieve gateway configuration\n" + regResp.getError());
	        			logger.warning("Failed to retrieve gateway configuration: " + regResp.getError());
	        		}
	        		
	        		/*System.out.println("Success");
		    		System.out.println("GW: " + ((Gateway)regResp.getReturnValue()).toString());
		    		System.out.println(regResp.getMessage());*/
		    } else {
		    		Cmd.p("Error: Failed to create account.\n" + regResp.getError());
		    		logger.warning("Failed to create account: " + regResp.getError());
		    }
			
		} else {
			Cmd.p("\n1. Sync with existing free service account.");
			Cmd.p("2. Delete existing free service account");
			String choiceStr = System.console().readLine("\nEnter choice: ");
			int choiceInt = verifyChoice(choiceStr, 2);
			
			if (choiceInt++ == -1 ) {
				Cmd.p("\nInvalid choice.");
				return;
			}
			
			if (choiceInt == 1) {
				logger.info("Syncing with free service");
				sync(gateways[0]);
			} else {
				// Delete account
				logger.info("Deleting free service account");
				RegisterResponse regResp = new RegisterAccount().deleteAccount(gateways[0].getUserName());
				
				if (regResp.success()) {
					Cmd.p("\n" + regResp.getMessage() + "\n");
					logger.info("Successfully deleted free service account");
				} else {
					// TODO - if can't persist delete request and keep attempting periodically or if next restart
					logger.warning("Failed to delete free service account: " + regResp.getError());
				}
				
				// even if the delete fails still remove config and rest AccountUUID
				logger.fine("Updating AccountUUID");
				cblStore.updateAccountUUID(accounts, "");
				logger.info("Saving gateway config");
				saveRemoteGateway(null);
			}
			
		}
	}
	
	
	
	/*
	private void syncWithRemoteV2() {
		Gateway[] gateways = null;
		
		try {
			gateways = loadSyncConfig("remote");
		} catch (Exception e) {
			System.err.println("\nError Loading Sync Gateway Config: " + e.getMessage());
			//e.printStackTrace();
			//Cmd.p("\nCould not load config, create new config");
			//createGatewayConfig();
			//return;
		}
		
		if (gateways == null || gateways.length == 0) {
			Cmd.p("\nIn order to Sync with the free service you need to register with your email address\n" +
					"and set a password. A unique identifier will be sent to you to complete the registration.\n" +
					"process.");
			Cmd.p("\n1. Enter an email address and password to create a new identifier.");
			Cmd.p("2. Enter an existing identifier if you have already registered.");
			Cmd.p("3. Resend your identifier to your email.");
			Cmd.p("4. Reset your password for your identifier.");
			Cmd.p("5. Go back to main menu.");
			
			String choice = System.console().readLine("\nEnter [1-5]: ");
			
			switch (choice) {
			case "1":
				String email = getEmail();
				String password = new String(System.console().readPassword("Enter a password for your account: "));
//TODO - Get RegisterResponse
				if (RegisterAccount.startRegistration(email, password).success()) {
					finishRegistration();
				} else {
					Cmd.p("There was a problem with trying to register your account");
					return;
				}
			case "2":
				finishRegistration();
				return;
			case "3":
				email = getEmail();
				
				if (RegisterAccount.resendEmail(email)) {
					finishRegistration();
					return;
				} else {
					Cmd.p("\nFailed to resend identifier");
					return;
				}
			case "4":
				//TODO FINISH HERE
				if (RegisterAccount.startResetPassword(getEmail())) {
					
					password = new String(System.console().readPassword("Enter a password for your account: "));
					
					if (RegisterAccount.finishResetPassword(password)) {
						
					}
				} else {
					
				}
				
				break;	
			case "5":
				return;
			default:
				Cmd.p("Invalid choice");
				return;
			}
			
			//syncWithLocal();
		} else {
			sync(gateways[0]);
		}
	}
	*/
	
	
	/*
	private boolean finishRegistration() {
		// UUID sent to email prompt for it
		String uuid = System.console().readLine("Your identifier was sent to you, enter it: ");
		
		if (uuid != null && !uuid.equalsIgnoreCase("")) {
//TODO - get RegisterResponse
			Gateway gateway = (Gateway)RegisterAccount.finishRegistration(uuid).getReturnValue();
			
			if (gateway != null) {
				Cmd.p("Successful registration");
				Cmd.p(gateway.toString());
				saveRemoteGateway(gateway);
				cblStore.updateAccountUUID(accounts, uuid);
				return true;
			} else {
				Cmd.p("Failed to register");
				return false;
			}
			
		} else {
			Cmd.p("Invalid entry..");
			return false;
		}
	}
	*/
	
	
	private void sync(Gateway gateway) {
		SyncGatewayClient.ReplicationStatus status = null;
	
		System.out.print("\nSyncing.");
		logger.info("Syncing with server: " + gateway.getServer() + ", protocol: " + gateway.getProtocol() +
				", port: " + gateway.getPort() + ", bucket: " + gateway.getBucket());
					
		if (gateway.getUserName() == null || gateway.getUserName().equalsIgnoreCase("")) {
			// use guest account
			status = cblStore.syncAccounts(gateway.getServer(), gateway.getProtocol(), gateway.getPort(), 
						gateway.getBucket(), accountsChanged);
		} else {	
			// use credentials
			status = cblStore.syncAccounts(gateway.getServer(), gateway.getProtocol(), gateway.getPort(), 
					gateway.getBucket(), gateway.getUserName(), gateway.getPassword(), accountsChanged);
		}
					
		while (status.isRunning()) {
			System.out.print(".");
						
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) { 
				logger.log(Level.WARNING, "Thread interrupted while sync was running: " + e.getMessage(), e);
				e.printStackTrace(); 
			}
		}
					
		System.out.print(".\n\n");
					
		Throwable pullError = status.getPullError();
		Throwable pushError = status.getPushError();
					
		if (pullError != null) {
			Cmd.p("Sync pull errors: " + pullError.getMessage());
			logger.warning("Sync pull errors: " + pullError.getMessage());
			pullError.printStackTrace();
		}
					
		if (pushError != null) {
			Cmd.p("Sync push errors: " + pushError.getMessage());
			logger.warning("Sync push errors: " + pushError.getMessage());
			pushError.printStackTrace();
		}
			
		logger.fine("Sync complete");
		Cmd.p("Sync Compete");
	}
	
	
	private Gateway createGatewayConfig() {
		
		String server = System.console().readLine("\nEnter hostname or ip: ");
		String port = System.console().readLine("Enter port or just <enter> to use default [4984]: ");
		String protocol = System.console().readLine("Enter protocol [http/https] or just <enter> to use default [http]: ");
		
		/*
		if (protocol.equalsIgnoreCase("https")) {
			// Easy way is to set keystore as System property
			
		}
		*/
		
		String bucket = System.console().readLine("Enter bucket name: ");
		String user = System.console().readLine("Enter username if needed or just <enter>: ");
		String pass = new String(System.console().readPassword("Enter password if needed or just <enter>: "));
		
		Gateway gw = new Gateway();
		gw.setServer(server);
		gw.setBucket(bucket);
		
		if (port !=null && port.length() > 0)
			try {
				gw.setPort(Integer.parseInt(port));
			} catch (NumberFormatException e) {
				Cmd.p("|nInvalid Port, config not saved..");
				return null;
			}
		else
			gw.setPort(4984);
		
		if (protocol !=null && protocol.length() > 0)
			gw.setProtocol(protocol);
		else
			gw.setProtocol("http");
		
		if (user !=null && user.length() > 0)
			gw.setUserName(user);
		
		if (pass !=null && pass.length() > 0)
			gw.setPassword(pass);
		
		Cmd.p("\nSaving Config");
		Gateways gateways = null;
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			logger.finest("Loading gateway configs from file");
			
			if (new File("./.password_vault_sync.json").exists()) {
				byte[] jsonData = Files.readAllBytes(Paths.get("./.password_vault_sync.json"));
				gateways = objectMapper.readValue(jsonData, Gateways.class);
				decryptPasswords(gateways);
			} else {
				gateways = new Gateways();
			}
			
			Gateway[] localGW = gateways.getLocal();
		
			if (localGW != null && localGW.length > 0) {
				logger.finest("Local gateways count: " + localGW.length);
				//don't allow duplicates
				for (int i=0; i<localGW.length; i++) {
					
					if (localGW[i].equals(gw)) {
						Cmd.p("\nGateway definition already exists");
						return localGW[i];
					}
				}
				
				Gateway[] newGW =  new Gateway[localGW.length+1];
				
				for (int i=0; i<localGW.length; i++) 
					newGW[i] = localGW[i];
				
				newGW[newGW.length-1] = gw;
				localGW = newGW;
			} else {
				localGW = new Gateway[1];
				localGW[0] = gw;
			}
			
			gateways.setLocal(localGW);
			encryptPasswords(gateways);
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			objectMapper.writeValue(new File("./.password_vault_sync.json"), gateways);
			
			logger.info("Gateway configs saved");
			Cmd.p("\nConfig Saved..");
		} catch(Exception e) {
			Cmd.p("\nError loading/saving gateways: " + e.getMessage());
			logger.log(Level.WARNING, "Error loading/saving gateways: " + e.getMessage(), e);
			e.printStackTrace();
			return null;
		} finally {
			decryptPasswords(gateways);
		}
		
		return gw;
	}
	
	
	private void saveRemoteGateway(Gateway gateway) {
		Gateways gateways = null;
		ObjectMapper objectMapper = new ObjectMapper();
		//String unencryptedPassword = gateway.getPassword();
		logger.finest("Saving remote gateway config");
		
		try {
			if (new File("./.password_vault_sync.json").exists()) {
				byte[] jsonData = Files.readAllBytes(Paths.get("./.password_vault_sync.json"));
				gateways = objectMapper.readValue(jsonData, Gateways.class);
				decryptPasswords(gateways);
			} else {
				gateways = new Gateways();
			}
			
			gateways.setRemote(gateway);  //only allow 1 remote definition so overwirte if exists
			encryptPasswords(gateways);
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			objectMapper.writeValue(new File("./.password_vault_sync.json"), gateways);
			
			Cmd.p("\nConfig Saved..");
			logger.config("Remote gateway config saved");
		} catch(Exception e) {
			Cmd.p("\nError loading/saving remote gateway: " + e.getMessage());
			logger.log(Level.WARNING, "Error loading/saving remote gateway: " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			// always revert back to unencrypted passsword for live Gateway Object
			decryptPasswords(gateways);
		}
	}
	
	
	private void deleteGatewayConfig(Gateway toDelete, Gateway[] gateways) {
		
		if (gateways == null || gateways.length < 1)
			return;
		
		Cmd.p("\nDeleteing gateway " + toDelete.getProtocol() + "://" + toDelete.getServer() +
						"/" + toDelete.getBucket());
		logger.info("Deleteing gateway " + toDelete.getProtocol() + "://" + toDelete.getServer() +
						"/" + toDelete.getBucket());
		
		if (gateways.length == 1) {
			gateways = null;
		} else {
			Gateway[] temp = new Gateway[gateways.length-1];
			
			for (int i=0, j=0; i<temp.length; i++, j++) {
				
				if (toDelete.equals(gateways[i]))
					j++;
				
				temp[i] = gateways[j];
			}
			
			gateways = temp;
		}
		
		Gateways gw = null;
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			
			if (new File("./.password_vault_sync.json").exists()) {
				byte[] jsonData = Files.readAllBytes(Paths.get("./.password_vault_sync.json"));
				gw = objectMapper.readValue(jsonData, Gateways.class);
				decryptPasswords(gw);
			} else {
				gw = new Gateways();
			}
			
			gw.setLocal(gateways);
			encryptPasswords(gw);
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			objectMapper.writeValue(new File("./.password_vault_sync.json"), gw);
			
			Cmd.p("\nGateway configuration deleted..");
			logger.info("Gateway configuration deleted");
			
		} catch(Exception e) {
			Cmd.p("\nError deleting Gateway configuration: " + e.getMessage());
			logger.log(Level.WARNING, "Error deleting Gateway configuration: " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			decryptPasswords(gw);
		}
	}
	
	
	public static Gateway[] loadSyncConfig(String type) throws Exception {
		
		byte[] jsonData = Files.readAllBytes(Paths.get("./.password_vault_sync.json"));
		ObjectMapper objectMapper = new ObjectMapper();
		Gateways gateWays = objectMapper.readValue(jsonData, Gateways.class);
		decryptPasswords(gateWays);
		
		switch (type) {
		case "local":
			return gateWays.getLocal();
		case "remote":
			Gateway remote = gateWays.getRemote();
			
			if (remote != null) {
				return new Gateway[] {remote};
			}
			
			return null;
		default:
			break;
		}
		
		return null;
	}
	
	
	private static void encryptPasswords(Gateways gateways) {
		Gateway remote = gateways.getRemote();
		
		if (remote != null) {
			remote.setPassword(new String(Utils.encodeBytes(AESEngine.getInstance()
					.encryptString(key, remote.getPassword()))));
		}
		
		Gateway[] local = gateways.getLocal();
		
		if (local != null) {
			for (Gateway gateway : local) {
				gateway.setPassword(new String(Utils.encodeBytes(AESEngine.getInstance()
						.encryptString(key, gateway.getPassword()))));
			}
		}
	}
	
	
	private static void decryptPasswords(Gateways gateways) {
		Gateway remote = gateways.getRemote();
		
		if (remote != null) {

			try {
				remote.setPassword(AESEngine.getInstance()
						.decryptBytes(key, Utils.decodeString(remote.getPassword())));
			} catch (Exception e) {
				System.err.println("Error decrypting password for remote service, " + e.getMessage());
				logger.log(Level.WARNING, "Error decrypting password for remote service, " + e.getMessage(), e);
				e.printStackTrace();
			}
		}
		
		Gateway[] local = gateways.getLocal();
		
		if (local != null) {
			for (Gateway gateway : local) {
				try {
					gateway.setPassword(AESEngine.getInstance()
							.decryptBytes(key, Utils.decodeString(gateway.getPassword())));
				} catch (Exception e) {
					System.err.println("Error decrypting password for local service, " + 
							gateway.getServer() + ", " + e.getMessage());
					logger.log(Level.WARNING, "Error decrypting password for local service, " + e.getMessage(), e);
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 moved to Utils 
	 
	public static String getAccountUUID() {
		String toReturn = null;
		
		try {
			Gateway[] remote = loadSyncConfig("remote");
			
			if (remote != null && remote.length > 0) {
				toReturn = remote[0].getUserName();
			}
			
		} catch (Exception e) {
			Cmd.p("Error opening sync config: " + e.getMessage());
			//e.printStackTrace();
		}
		
		return (toReturn == null ? "" : toReturn);
	}
	*/
	
	
	private int verifyChoice(String choice, int max) {
		int gw = -1;
		
		try {
			gw = Integer.parseInt(choice);
		} catch(NumberFormatException e) {
			Cmd.p("\nError parsing: " + e.getMessage());
			return -1;
		}
		
		if (--gw < 0 || gw >= max)
			return -1;
		
		return gw;
	}
	
	
	private String getEmail() {
		boolean valid = false;
		
		while (!valid) {
			String email = System.console().readLine("\nEnter the email address you wish to register: ");
			valid = EmailValidator.getInstance().isValid(email);
			
			if (valid) {
				return email;
			} else {
				Cmd.p("\nThe email address entered is not a valid address.");
			}
		}
		
		return null;
	}
	
	/*
	public static void main(String args[]) throws Exception{
		//SyncCmd sc = new SyncCmd(null, null);
		//sc.run();
		
		//SyncCmd.getEmail();
		System.out.println(key);
		String pass = "password";
		byte[] encryptedPass = AESEngine.getInstance().encryptString(key, pass);
		System.out.println("pass=" + pass);
		System.out.println("encypted pass=" + new String(encryptedPass));
		//System.out.println("encoded pass=" + new String(Utils.encodeBytes(pass.getBytes())));
		String encodeEncrypt = new String(Utils.encodeBytes(encryptedPass));
		System.out.println("encoded encrypted pass=" + encodeEncrypt);
		//System.out.println("encrypted encoded pass=" + 
		//		AESEngine.getInstance().encryptString(key, new String(Utils.encodeBytes(pass.getBytes()))));
		System.out.println("decrypted decoded pass=" + 
				AESEngine.getInstance().decryptBytes(key, Utils.decodeString(encodeEncrypt)));
	}
	*/
}
