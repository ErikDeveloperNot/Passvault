package com.passvault.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
//import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;

import com.passvault.crypto.AESEngine;
import com.passvault.crypto.CryptEngine;
import com.passvault.model.Gateway;
import com.passvault.ui.text.SyncCmd;

import android.util.Base64;

public class Utils {
	
	private static String platform;
	private static Logger logger;
	private static AccountUUIDResolver uuidResolver;
	
	public static final String BASE_URL = "/PassvaultServiceRegistration";
	public static final String KEY_STORE_JKS = "com/passvault/ssl/passvault_store.jks";
	public static final String KEY_STORE_BKS = "com/passvault/ssl/passvault_store.bks";
	public static final String KEY_STORE_PASSWORD = "passvault";
	public static final String GITHUB_REG_URL = "https://api.github.com/repos/ErikDeveloperNot/Passvault/" +
												"contents/Passvault/config/RegistrationServer.json?ref=master";
	public static final int CONNECTION_TIMEOUT = 30;
	public static final int READ_TIMEOUT = 30;
	
	static {
		logger = Logger.getLogger("com.passvault.util");
		
		try {
			Class.forName("com.erikdeveloper.passvault.couchbase.AndroidCBLStore");
			platform = "mobile";
		} catch(Exception e) {
			platform = "desktop";
		}
		
		if (platform.equals("desktop")) {
			uuidResolver = new AccountUUIDResolver() {
				
				@Override
				public String getAccountUUID() {
					String toReturn = null;
					
					try {
						Gateway[] remote = SyncCmd.loadSyncConfig("remote");
						
						if (remote != null && remote.length > 0) 
							toReturn = remote[0].getUserName();
						
					} catch (Exception e) {
						logger.log(Level.WARNING, "Error opening sync configuration file: " + e.getMessage(), e);
					}
					
					return (toReturn == null ? "" : toReturn);
				}
			};
		}
	}

	/*
	 * account file will be saved in the format of account_name:password:oldpassword:user_name:updateTime|..
	 * 
	 * to parse split line by pipe delimeter (|) for each account, and then by colon (:) for 
	 * fields
	 * 
	 * crypto stuff copied from  @author www.codejava.net from
	 * http://www.codejava.net/coding/file-encryption-and-decryption-simple-example
	 * 
	 */
	public static void loadAccounts(String file, String key, List<Account> accounts) {
		File accountFile = new File(file);
		
		if (accountFile.exists()) {
			logger.finest("Loading accounts from: " + file);
			CryptEngine aesEngine = AESEngine.getInstance();
			String decryptedContent = aesEngine.decryptFile(key, accountFile);
			
			if (decryptedContent == null || decryptedContent.length() == 0)
				return;
			
			//split into records
            String[] records = decryptedContent.split(Account.RECORD_DELIMETER);
            logger.finest("Number of accounts: " + records.length);
         
            //add each record to accounts
            for (String record : records) {
				String[] recordFields = record.split(Account.FIELD_DELIMIETER);
				Account account = null;

				/*
				System.out.println("length=" + recordFields.length + " for account:" + recordFields[0]);	
				if (recordFields[0].contains("test"))
					System.out.println("0="+recordFields[0]+", 1="+recordFields[1]+", 2="+recordFields[2]+", 3="+recordFields[3]);
				*/
				
				//password can be blank
				if (recordFields.length >= 4) {
					long updateTime;
					
					if (recordFields.length == 4)
						updateTime = System.currentTimeMillis();
					else
						updateTime = Long.parseLong(recordFields[4]);
						
					account = new Account(recordFields[0], recordFields[3], recordFields[1], recordFields[2], "", updateTime);
				} else {
					System.err.println("Invalid number of record fields for account " + recordFields[0] + recordFields.length);
					logger.warning("Invalid number of record fields for account " + recordFields[0] + recordFields.length);
				}
				
				if(account.getPass().equalsIgnoreCase(Account.BLANK_PASSWORD)) 
					account.setPass("");
				
				if(account.getOldPass().equalsIgnoreCase(Account.BLANK_PASSWORD)) 
					account.setOldPass("");
				
				logger.fine("Adding account: " + account.getName());
				accounts.add(account);
			}
		}
	}
	
	public static void saveAccounts(String file, String key, List<Account> accounts) {
		StringBuilder decryptedAccounts = new StringBuilder();
		logger.fine("Saving accounts to: " + file);
		
		//add each record to String
		synchronized (accounts) {
	        for (Account account : accounts) {
				
				if (decryptedAccounts.length() > 0)
					decryptedAccounts.append("|");
				/*	
				if (account.getPass().length()==0) 
					account.setPass(Account.BLANK_PASSWORD);
				
				if (account.getOldPass().length()==0) 
					account.setOldPass(Account.BLANK_PASSWORD);
				*/
				
				//book shelf record with account and user name since they can't be zero
				decryptedAccounts.append(account.getName() + Account.FIELD_DELIMIETER + 
						account.getPass() + Account.FIELD_DELIMIETER + account.getOldPass() +
						Account.FIELD_DELIMIETER + account.getUser() + Account.FIELD_DELIMIETER + 
						account.getUpdateTime());
			}
		}
        
        CryptEngine aesEngine = AESEngine.getInstance();
        byte[] encryptedAccounts = aesEngine.encryptString(key, decryptedAccounts.toString());
    		FileOutputStream fos = null;
    			
        try {
	        	File datFile = new File(file);
	        	//if already exists backup
	        	if (datFile.exists()) {
	        		logger.finest("Renaming old data file");
	        		datFile.renameTo(new File(file + "." + System.currentTimeMillis()));
	        	}
	        		
	        	logger.finest("Saving new data file");
	        	fos = new FileOutputStream(new File(file));
	        	fos.write(encryptedAccounts);
		
        } catch (Exception e) {
        		logger.log(Level.WARNING, "Error saving accounts data file: " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch(IOException ioe) {}
		}
	}
	
	
	public static KeyStore getKeyStore(String store, String password, String type) {
		KeyStore toReturn = null;
		logger.info("Loading keystore: " + store);
		InputStream stream = Utils.class.getClassLoader().getResourceAsStream(store);					
		
		try {
			toReturn = KeyStore.getInstance(type.toUpperCase());
			toReturn.load(stream, password.toCharArray());
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error loading keystore: " + e.getMessage(), e);
		}
		
		return toReturn;
	}
	
	
	public static byte[] decodeString(String toDecode) {	
		
		if (toDecode == null || toDecode.equals("")) {
			//return Base64.getDecoder().decode("");
			return Base64.decode("", Base64.DEFAULT);
		} else {
			//return Base64.getDecoder().decode(toDecode);
			return Base64.decode(toDecode, Base64.DEFAULT);
		}
	}	
	
	
	public static byte[] encodeBytes(byte[] toEncode) {
		
		if (toEncode == null) {
			//return Base64.getEncoder().encode(new byte[]{});
			return Base64.encode(new byte[]{}, Base64.DEFAULT);
		} else {
			//return Base64.getEncoder().encode(toEncode);
			return Base64.encode(toEncode, Base64.DEFAULT);
		}
		
	}
	
	
	
	public static SSLSocketFactory createSSLSocketFactoryWitDefaultTrustManager(KeyStore store) {
		SSLContext ctx = null;
		logger.fine("Getting SSLSocketFactory with default TrustManagerFactory and supplied keystore");
		
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(store);
			
		    ctx = SSLContext.getInstance("TLSv1.2");
		    ctx.init(null, tmf.getTrustManagers(), null);
		} catch (Exception e) {
			System.err.println("Error creating default TrustManager, using default, " + e.getMessage());
			logger.log(Level.WARNING, "Unable to create SSLSocketFactory with supplied keystore: " +
					e.getMessage(), e);
			e.printStackTrace();
		}
		
		if (ctx != null)
			return ctx.getSocketFactory();
		else
			return (SSLSocketFactory)SSLSocketFactory.getDefault();
					
	}
	
	
	public static SSLSocketFactory createSSLSocketFactoryWitBasicTrustManager(final KeyStore store) {
		SSLSocketFactory sslSocketFactory = null;
		X509TrustManager trustManager = null;
		SSLContext ctx = null;
		logger.fine("Getting Basic SSLSocketFactory with supplied keystore");
		
		try {
			ctx = SSLContext.getInstance("TLSv1.2");
			
			trustManager = new X509TrustManager() {
		    	
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					List<X509Certificate> certs = new ArrayList<>();
					
					try {
						Enumeration<String> aliases = store.aliases();
						
						while (aliases.hasMoreElements()) {
							String alias = aliases.nextElement();
							
							if (store.isCertificateEntry(alias))
								certs.add((X509Certificate)store.getCertificate(alias));
						}
					} catch (KeyStoreException e) {
						System.err.println("Error accessing keystore, " + e.getMessage());
						logger.log(Level.WARNING, "Error accessing keystore: " + e.getMessage(), e);
						e.printStackTrace();
					}
					
					if (certs.isEmpty()) {
						logger.fine("Returning 0 certificates");
						return new X509Certificate[0];
					} else {
						X509Certificate[] toReturn = new X509Certificate[certs.size()];
						int i=0;
						
						for (X509Certificate x509Certificate : certs) {
							logger.finest("Adding certificate: " + x509Certificate.getSubjectDN().getName());
							toReturn[i++] = x509Certificate;
						}
						
						logger.fine("Returning " + toReturn.length + " certificate(s)");
						return toReturn;
						//return (X509Certificate[])certs.toArray();
					}

				}

				// simple implementation, doesn't take chains into account plus a number of other factors
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					logger.fine("Checking certificate for authType=" + authType);
					
					try {
						for (X509Certificate x509Certificate : chain) {
							logger.finest("Checking certificate: " + x509Certificate.getSubjectDN().getName());
							Enumeration<String> aliases = store.aliases();
							
							while (aliases.hasMoreElements()) {
								String alias = aliases.nextElement();
								
								if (store.isCertificateEntry(alias)) {
									X509Certificate storeCert = (X509Certificate)store.getCertificate(alias);
									
									if (storeCert.equals(x509Certificate)) {
										logger.info("Certificate found in truststore");
										return;
									} else {
										//System.out.println("\\n\\n>> CERTS NOT EQUAL\\n\\n");
									}
								}
							}
						}
					} catch (KeyStoreException e) {
						System.err.println("Error accessing keystore, " + e.getMessage());
						logger.log(Level.WARNING, "Error accessing keystore: " + e.getMessage(), e);
						e.printStackTrace();
					}
					
					logger.warning("Certificate not found in truststore, can't establish connection");
					throw new CertificateException("Certificate Not Found!!!");
				}
				
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					//do notta
				}
			};
		    
			ctx.init(null, new TrustManager[] {trustManager}, null);
			sslSocketFactory = ctx.getSocketFactory();
		} catch (Exception e) {
			System.err.println("Error creating basic TrustManager, using default, " + e.getMessage());
			logger.log(Level.WARNING, "Unable to create basice SSLSocketFactory with supplied keystore: " +
					e.getMessage(), e);
			e.printStackTrace();
		}
		
		if (sslSocketFactory != null)
			return sslSocketFactory;
		else
			return (SSLSocketFactory)SSLSocketFactory.getDefault();
	}
	
	/*
	public static String getAccountUUID() {
		String accountUUID = null;
		
		if (platform.equalsIgnoreCase("desktop")) {
			String toReturn = null;
			
			try {
				Gateway[] remote = SyncCmd.loadSyncConfig("remote");
				
				if (remote != null && remote.length > 0) {
					toReturn = remote[0].getUserName();
				}
				
			} catch (Exception e) {
				//System.err.println("Error opening sync config: " + e.getMessage());
				logger.log(Level.WARNING, "Error opening sync configuration file: " + e.getMessage(), e);
				//e.printStackTrace();
			}
			
			accountUUID = (toReturn == null ? "" : toReturn);
		} else {
			// TODO will come from properties
			accountUUID = "LookAtUtilsIfYouForget";
		}
		
		return accountUUID;
	}
	*/
	
	public static String getAccountUUID() {
		return uuidResolver.getAccountUUID();
	}
	
	
	public static void setAccountUUIDResolver(AccountUUIDResolver uuidResolver) {
		Utils.uuidResolver = uuidResolver;
	}
	
	
	/*
	 * Helpers for REST calls
	 */
	public static Invocation.Builder createBuilder(String registerServer, String baseURL, String[] paths) {
		URI baseURI = getBaseURI(registerServer, baseURL);
		Client client = getClient(baseURI.getScheme());
		WebTarget target = client.target(getBaseURI(registerServer, baseURL));
        
        // paths[] need to make sure path objects are in the correct order
        for (String path : paths) {
        		logger.finest("Adding path: " + path);
			target = target.path(path);
		}
        
        //System.out.println(target.getUri().getPath());
        return target.request();
	}
	
	
	private static Client getClient(String scheme) {
		Client client = null;
		logger.fine("Getting client for protocol: " + scheme);
		
		String platform;
		
		try {
			Class.forName("com.erikdeveloper.passvault.couchbase.AndroidCBLStore");
			platform = "mobile";
		} catch(Exception e) {
			platform = "desktop";
		}
		
		if (scheme.equalsIgnoreCase("https")) {
			KeyStore store = null;
			
			if (platform.equalsIgnoreCase("mobile"))
				store = Utils.getKeyStore(KEY_STORE_BKS, KEY_STORE_PASSWORD, "BKS");
			else
				store = Utils.getKeyStore(KEY_STORE_JKS, KEY_STORE_PASSWORD, "JKS");
			
			SslConfigurator sslConfig = SslConfigurator.newInstance()
					.trustStore(store)
					.trustStorePassword(KEY_STORE_PASSWORD);
			/*
			SslConfigurator sslConfig = SslConfigurator.newInstance()
			        .trustStoreFile("/opt/ssl/keystores/passvault_store.jks")
			        .trustStorePassword("passvault");
			        */
			SSLContext sslContext = sslConfig.createSSLContext();
			client = ClientBuilder.newBuilder().sslContext(sslContext).build();
		} else {
			ClientConfig config = new ClientConfig();
	        client = ClientBuilder.newClient(config);
		}
		
		logger.fine("Returning client");
		return client;
	}

    private static URI getBaseURI(String registerServer, String baseURL) {
    		String protocol = getProtocol(registerServer);
    		//return UriBuilder.fromUri(protocol + "://" + registerServer + baseURL).build();
    		return UriBuilder.fromUri(protocol + "://" + registerServer).build();
    }
    
    private static String getProtocol(String registerServer) {
    		
	    	if (registerServer.contains("8080")) 
	    		return "http";
	    	else 
	    		return "https";
    }
 }
