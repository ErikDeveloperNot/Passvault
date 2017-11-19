package com.passvault.util.data.file;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.ws.soap.AddressingFeature.Responses;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passvault.util.Utils;
import com.passvault.util.model.syncserver.SyncRequestFinal;
import com.passvault.util.model.syncserver.SyncRequestInitial;
import com.passvault.util.model.syncserver.SyncResponseInitial;
import com.passvault.util.register.RegisterResponse;
import com.passvault.util.sync.AccountsChanged;

public class PassvaultSyncServerClient {

	private String host;
	private String protocol;
	private int port;
	private String path;
	private String user;
	private String password;
	private AccountsChanged accountsChanged;
	
	private Logger logger;
	
	
	public PassvaultSyncServerClient(String host, String protocol, int port, String path, String user,
			String password, AccountsChanged accountsChanged) {
		
		this.host = host;
		this.protocol = protocol;
		this.port = port;
		this.path = path;
		this.user = user;
		this.password = password;
		this.accountsChanged = accountsChanged;
		logger = Logger.getLogger("com.passvault.util.data.file");
	}
	
	
	public SyncResponseInitial initialSync(SyncRequestInitial initialSyncRequest) {
		Response response = null;
		SyncResponseInitial initalResponse = null;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			logger.finest("Sending initialRequest");
			response = sendPOST(initialSyncRequest, (path + "/sync-initial").split("/"));
		} catch (Exception e) {
			logger.warning("Error sending initialRequest: " + e.getMessage());
			e.printStackTrace();
			initalResponse = new SyncResponseInitial();
			initalResponse.setResponseCode(Codes.ERROR);
			return initalResponse;
		}
		
		logger.fine("Http status=" + response.getStatus());
		
		if (response.hasEntity()) {
			// successfully return
			String responseString = null;
			
			try {
				responseString = response.readEntity(String.class);
				initalResponse = mapper.readValue(responseString, SyncResponseInitial.class);
			} catch (IOException e) {
				logger.warning("Error parsing initialRequest: " + e.getMessage());
				logger.warning("Returned message:\n" + responseString);
				e.printStackTrace();
				initalResponse = new SyncResponseInitial();
				initalResponse.setResponseCode(Codes.ERROR);
			}
		} else {
			logger.warning("No Entity returned from call");
			initalResponse = new SyncResponseInitial();
			initalResponse.setResponseCode(Codes.ERROR);
		}
		
		return initalResponse;
	}
	

	public String syncFinal(SyncRequestFinal finalRequest) {
		String toReturn = null;;
		Response response = null;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			logger.finest("Sending finalRequest");
			response = sendPOST(finalRequest, (path + "/sync-final").split("/"));
		} catch (Exception e) {
			logger.warning("Error sending finalRequest: " + e.getMessage());
			e.printStackTrace();
			return "Error syncing accounts: " + e.getMessage();
		}
		
		logger.fine("Http status=" + response.getStatus());
		
		if (response.getStatus() < 300) {
			// successfully return
			toReturn = null;
		} else {
			toReturn = "Error syncing accounts";
			
			if (response.hasEntity()) {
				String responseString = response.readEntity(String.class);
				logger.warning("Error with finalSync: " + responseString);
				toReturn += ": " + responseString;
			}
		}
		
		return toReturn;
	}
	
	
	private Response sendPOST(Object sendModel, String[] paths) throws Exception {
		
		Invocation.Builder builder = Utils.createBuilder(host + ":" + port, Utils.BASE_URL, paths);
		ObjectMapper sendMapper = new ObjectMapper();
        Response response = null;
        
        try {
			response = builder.post(Entity.json(sendMapper.writeValueAsString(sendModel)));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to send POST request: " + e.getMessage(), e);
			e.printStackTrace();
			throw e;
		}
        
        return response;
	}
	
	
	
	public static class Codes {
		public static final int SUCCESS = 0;
		public static final int ERROR = 1;
		public static final int ACCOUNT_ALREADY_EXISTS = 2;
		public static final int ACCOUNT_DOES_NOT_EXIST = 3;
		public static final int ACCOUNT_ALREADY_LOCKED = 4;
		public static final int INVALID_PASSWORD = 5;
		public static final int BACK_END_TIMEOUT = 6;
		public static final int SERVER_NOT_INITIALIZED = 7;
		public static final int ACCOUNT_ADDED = 8;
		public static final int LOCK_SUCCESS = 100;
		
		public static String getErrorStringForCode(int code) {
			switch (code) {
			case SUCCESS:
				return "The operation completed successfully";
			case ERROR:
				return "There was an error in the operation";
			case ACCOUNT_ALREADY_EXISTS:
				return "Account name already exists";
			case ACCOUNT_DOES_NOT_EXIST:
				return "Account name does not exist";
			case ACCOUNT_ALREADY_LOCKED:
				return "The account is locked by another request";
			case INVALID_PASSWORD:
				return "Invalid password";
			case BACK_END_TIMEOUT:
				return "The operation timed out";
			case SERVER_NOT_INITIALIZED:
				return "The Sync Server is not ready to accept requests";
			case ACCOUNT_ADDED:
				return "The account was added";
			case LOCK_SUCCESS:
				return "The account has been locked";
			default:
				return "Unkown result code";
			}
		}
	}
}
