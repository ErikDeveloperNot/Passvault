package com.passvault.data.couchbase;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Database.ChangeListener;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.ReplicationFilter;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.CouchbaseLiteHttpClientFactory;
import com.couchbase.lite.support.PersistentCookieJar;
import com.couchbase.lite.util.Log;
import com.passvault.util.Utils;
import com.passvault.util.couchbase.SyncClient;
import com.passvault.util.sync.AccountsChanged;

public class SyncGatewayClient implements SyncClient {

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 4984;
	public static final String DEFAULT_BUCKET = "passvault";
	public static final String DEFAULT_PROTOCOL = "http";
	private static final String INTERNAL_PREFIX = "__";
	
	private static boolean liverQueryRunning = false;
	private static boolean changeListenerRunning = false;
	
	private String host;
	private String protocol;
	private String user;
	private String userPassword;
	private String bucket;
	private int port;
	private Database database;
	private Database.ChangeListener changeListener;
	private AccountsChanged accountsChanged;
	private Replication pusher, puller;
	private ReplicationStatus replicationStatus;
	
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.util.couchbase");
	}
	
	public SyncGatewayClient(Database database) {
		this(database, DEFAULT_HOST, DEFAULT_PROTOCOL, DEFAULT_PORT, DEFAULT_BUCKET);
	}
	
	public SyncGatewayClient(Database database, String host, String protocol, int port, String bucket) {
		this.database = database;
		this.host = host;
		this.protocol = protocol;
		this.port = port;
		this.bucket = bucket;
		
		setLogging(database.getManager());
		
		replicationStatus = new ReplicationStatus();
		addCallbackDatabaseChangeListenser();
		startConflictLiveQuery();
	}
	
	public SyncGatewayClient(Database database, String host, String protocol, int port, String bucket, 
			String user, String password) {
		
		this.database = database;
		this.host = host;
		this.protocol = protocol;
		this.port = port;
		this.bucket = bucket;
		
		if (user != null && !user.contentEquals("")) {
			this.user = user;
			this.userPassword = password;
		}
		
		setLogging(database.getManager());
		
		replicationStatus = new ReplicationStatus();
		addCallbackDatabaseChangeListenser();
		startConflictLiveQuery();
		
	}
	
	
	private void setLogging(Manager manager) {
		Level level;
		int logLevel;
		
		if (System.getProperty("com.passvault.sync.logging", "off").equalsIgnoreCase("debug")) {
			level = Level.FINEST;
			logLevel = Log.VERBOSE;
		} else { 
			level = Level.FINEST;
			logLevel = Log.ERROR;
		}
		
		//logger.setLevel(level);
		/*	
		for(Handler h : logger.getParent().getHandlers()) {
			if(h instanceof ConsoleHandler){
	    	        h.setLevel(level);
	    	        //System.out.println("handler=" + h);
	    	    }
	    	}
		*/

		Manager.enableLogging("Sync", logLevel);
		Manager.enableLogging("ChangeTracker", logLevel);
	}
	
	
	
	public void sync() {
		URL url = null;
		
		try {
			logger.finest("Sync URL: " + protocol + "://" + host + ":" + port + "/"+bucket);
			url = new URL(protocol, host, port, "/"+bucket);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

// may chanege sending accountUUID to constructors but just to test
String accountUUID = Utils.getAccountUUID();
Map<String, Object> params = new HashMap();
params.put("accountUUID", accountUUID);


		// only send docs with the current accountUUID and dont start with '__'
		database.setFilter("accountUUIDFilter", new ReplicationFilter() {
					
			@Override
			public boolean filter(SavedRevision revision, Map<String, Object> params) {
				String accountUUID = (String) params.get("accountUUID");
				String docID = revision.getDocument().getId();
				logger.finest("doc id=" + docID + ", " + accountUUID + " returning: " +
						(docID.contains(accountUUID) ? true : false) + ", internal __ = " +
						(docID.startsWith(INTERNAL_PREFIX) ? true : false));
				return (docID.contains(accountUUID) ? true : false)  &&
						(!docID.startsWith(INTERNAL_PREFIX));
			}
		});
		
		if (protocol.equalsIgnoreCase("https")) {
			logger.finest("Using https");
			/*
			 * for desktop version rely on javax.net.ssl.trustStore and javax.net.ssl.trustStorePassword
			 * for mobile - TODO
			 */
			String platform;
			
			try {
				Class.forName("com.erikdeveloper.passvault.couchbase.AndroidCBLStore");
				platform = "mobile";
			} catch(Exception e) {
				platform = "desktop";
			}
			
			logger.finest("platform: " + platform);
			
			SSLSocketFactory sslSocketFactory = null;
			X509TrustManager trustManager = null;
		
			try {
				KeyStore store = null;
				InputStream stream = null;
				
				if (platform.equalsIgnoreCase("mobile")) {
					stream = this.getClass().getClassLoader().getResourceAsStream("com/passvault/ssl/passvault_store.bks");
					store = KeyStore.getInstance("BKS");
				} else {
					stream = this.getClass().getClassLoader().getResourceAsStream("com/passvault/ssl/passvault_store.jks");
					store = KeyStore.getInstance("JKS");
				}

				store.load(stream, "passvault".toCharArray());
			
				
				// use default TrustManager
				sslSocketFactory = Utils.createSSLSocketFactoryWitDefaultTrustManager(store);
				
				// use basic TrustManager
				//sslSocketFactory = Utils.createSSLSocketFactoryWitBasicTrustManager(store);
			} catch (Exception e) {
				e.printStackTrace();
			}
				
					
			CouchbaseLiteHttpClientFactory clientFactory = new CouchbaseLiteHttpClientFactory(new PersistentCookieJar(database));
			
			if (sslSocketFactory == null) {
				logger.info("Unable to load keystore, using default SSLSocketFactory");
				
				// fall backs if unable to load keystore from classpath
				if (platform.equalsIgnoreCase("desktop")) {
					clientFactory.setSSLSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault());
				} else {
					clientFactory.setSSLSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault());
				}
				
			} else {
				clientFactory.setSSLSocketFactory(sslSocketFactory);
			}
			
			
			// Test
			/*
			try {
				SSLSocket soc = (SSLSocket)sslSocketFactory.createSocket("node1.user1.com", 4984);
				soc.startHandshake();
				Thread.sleep(60_000L);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			//end test
			
			database.getManager().setDefaultHttpClientFactory(clientFactory);
		}
		
		pusher = database.createPushReplication(url);
		pusher.setContinuous(false); 
		pusher.setFilter("accountUUIDFilter");
		pusher.setFilterParams(params);
	
		puller = database.createPullReplication(url);
		puller.setContinuous(false); 

		if (user != null && !user.contentEquals("")) {
			logger.info("Syncing with user: " + user);
			Authenticator authenticator = AuthenticatorFactory.createBasicAuthenticator(user, userPassword);
			pusher.setAuthenticator(authenticator);
			puller.setAuthenticator(authenticator);
		}
		
		
		pusher.start();
		logger.fine("Done starting pusher");
		puller.start();
		logger.fine("Done starting puller");
	}
	
	
	private Long getUpdateTime(Object time) {
		
		if (time instanceof Double)
			return ((Double) time).longValue();
		else
			return (Long)time;
	}
	
	
	private void addCallbackDatabaseChangeListenser() {
		
		if (changeListenerRunning)
			return;
		
		logger.info("Starting ChangeListener....");
		changeListenerRunning = true;
		
		changeListener = new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {

                if(!event.isExternal()) {
                		logger.finest(" -- NOT an EXTERNAL event");
					
                		if (accountsChanged != null)
						accountsChanged.onAccountsChanged();
					
                    return;
                }
                
                String accountUUID = Utils.getAccountUUID();
                logger.finest("-- CURRENT ACCOUNT UUID = " + accountUUID + ", number of changes=" +event.getChanges().size());
                
                for(final DocumentChange change : event.getChanges()) {
                		logger.finest("Working on document: " + change.getDocumentId());
                		Document changedDoc = database.getExistingDocument(change.getDocumentId());

                		if(!change.isCurrentRevision()) {
	                    	continue;
	                	}

	                	
	                	if (changedDoc == null) {
	                		logger.finest(" Document does not exist in database: " + change.getDocumentId());
	                		continue;
	                	}
                    
                    
                    if (!changedDoc.getProperty("AccountUUID").equals(accountUUID)) {
                    		logger.finest("-- accountUUIDs are not equal, changedDoc=" + 
                    				changedDoc.getProperty("AccountUUID") + ", current accoutUUID=" + accountUUID);
                    		// see if the db already has this account in it
                    		Document currentDoc = database.getExistingDocument(accountUUID + changedDoc.getProperty("AccountName"));
                    		
                    		if (currentDoc != null) {
                    			// doc already exists, compare UpdateTime property
                    			Long currentDocUpdateTime = getUpdateTime(currentDoc.getProperty("UpdateTime"));
                    			Long changedDocUpdateTime = getUpdateTime(changedDoc.getProperty("UpdateTime"));
                    			
                    			if (currentDocUpdateTime < changedDocUpdateTime) {
                    				// update current doc
                    				logger.info("Sync'd account updated later, updating existing account");
                    				Map<String, Object> content = changedDoc.getUserProperties();
                    				content.put("AccountUUID", accountUUID);
                    				UnsavedRevision newRev = currentDoc.createRevision();
                    				newRev.setUserProperties(content);
                    				
                    				logger.finest("Saving document: " + currentDoc.getId());
                    				
                    				try {
                    					newRev.save();
								} catch (CouchbaseLiteException e) {
									logger.warning("Error updating document: " + changedDoc.getProperty("AccountName"));
									e.printStackTrace();
									// don't delete anything
									continue;
								}
                    			} 
                    			
                    		} else {
                    			// create new document with changedDoc properties
                    			logger.info("Creating new aacount: " + changedDoc.getProperty("AccountName"));
                    			currentDoc = database.getDocument(accountUUID + changedDoc.getProperty("AccountName"));
                    			Map<String, Object> content = changedDoc.getUserProperties();
                    			content.put("AccountUUID", accountUUID);
                				
                				try {
								currentDoc.putProperties(content);
							} catch (CouchbaseLiteException e) {
								logger.warning("Error updating document: " + changedDoc.getProperty("AccountName"));
								e.printStackTrace();
								// don't delete anything
								continue;
							}
                    		}
                    		
                    		// delete the changedDoc
                    		try {
                    			logger.finest("deleting temporary document: " + changedDoc.getId());
							changedDoc.delete();
						} catch (CouchbaseLiteException e) {
							logger.warning("Error deleting document: " + changedDoc.getProperty("AccountName"));
							e.printStackTrace();
						}
                    } else {
                    		logger.fine("Account UUIDs are equal");
                    }
                    
                    // Going to move this down below all the change docs so it only runs once
                    // ******* MIGHT BREAK Android TIMING ***********
                    /*
                    if (accountsChanged != null)
                			accountsChanged.onAccountsChanged();
                    */
                    
                    //appears to not be need test since accountsChanged gets called either way, commenting out
                    /*
                    if (changedDoc == null || changedDoc.isDeleted()) {
                    	//System.out.println("Document is DELETED, need to refresh UI");
                    	
	                    	if (accountsChanged != null)
	                    		accountsChanged.onAccountsChanged();

                        continue;
                    } else {
                    	//System.out.println("Document: " + changedDoc.getId() + 
                    	//		" new rev:" + changedDoc.getCurrentRevisionId());
                    	
                    	if (accountsChanged != null)
                    		accountsChanged.onAccountsChanged();
                    }
                    */
                }
                
                if (accountsChanged != null)
        				accountsChanged.onAccountsChanged();
            }
        };
		
		logger.info("Adding database change listener");
		database.addChangeListener(changeListener);
	
	}
	
	
	private void startConflictLiveQuery() {
		
		if (liverQueryRunning)
			return;
		
		logger.info("Starting LiveQuery....");
		liverQueryRunning = true;
		
		LiveQuery conflictsLiveQuery = database.createAllDocumentsQuery().toLiveQuery();
		conflictsLiveQuery.setAllDocsMode(Query.AllDocsMode.ONLY_CONFLICTS);
		conflictsLiveQuery.addChangeListener(new AccountTimeBasedConflictResolver());
		conflictsLiveQuery.start();
	}
	
	
	/*
	 * set AccountsChanged call back in a chained method pattern
	 */
	public SyncGatewayClient setAccountsChanged(AccountsChanged accountsChanged) {
		this.accountsChanged = accountsChanged;
		return this;
	}
	
	
	/*
	 * set database change listener in a chained method pattern
	 */
	public SyncGatewayClient setDatabaseChangeListener(ChangeListener changeListener) {
		database.removeChangeListener(this.changeListener);
		this.changeListener = changeListener;
		database.addChangeListener(changeListener);
		return this;
	}
	
	
	public ReplicationStatus getReplicationStatus() {
		return replicationStatus;
	}
	
	
	public class ReplicationStatus implements com.passvault.util.sync.ReplicationStatus {
		
		public boolean isRunning() {
logger.info("+++++++++++++++ pusher status=" + pusher.getStatus().toString() + ", puller status=" + puller.getStatus().toString());
			return (puller.isRunning() || pusher.isRunning());
		}

		public Throwable getPullError() {
			return puller.getLastError();
		}
		
		public Throwable getPushError() {
			return pusher.getLastError();
		}
	}

}
