package com.passvault.util.register;


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

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passvault.util.Utils;
import com.passvault.util.model.DeleteRequest;
import com.passvault.util.model.Gateway;
import com.passvault.util.model.GithubContent;
import com.passvault.util.model.RegistrationRequest;
import com.passvault.util.model.RegistrationServer;
import com.passvault.util.model.RegistrationUUID;


// Used to make REST calls to registration service
public class RegisterAccount {
	
	private static final String VERSION_1 = "v1.0";
	private static final String VERSION_2 = "v2.0";
	private static final String BASE_URL = "/PassvaultServiceRegistration";
	private static final String KEY_STORE_JKS = "com/passvault/ssl/passvault_store.jks";
	private static final String KEY_STORE_BKS = "com/passvault/ssl/passvault_store.bks";
	private static final String KEY_STORE_PASSWORD = "passvault";
	private static final String GITHUB_REG_URL = "https://api.github.com/repos/ErikDeveloperNot/Passvault/" +
												"contents/Passvault/config/RegistrationServer.json?ref=master";
	private static Logger logger;

	private String registerServer;
	
	
	static {
		logger = Logger.getLogger("com.passvault.util.regiter");
	}
	
	
	//used by command line client System property must be set
	public RegisterAccount() {
		this(System.getProperty("com.passvault.register.server", null));
	}
	
	public RegisterAccount(String registerServer) {
		this.registerServer = registerServer;
		logger.info("Register server set to: " + registerServer);
	}
	
	
	/*
	 *  Version 1 Registration
	 */
	public RegisterResponse registerV1(String email, String password) {
		RegisterResponse regResp = new FinishRegistrationResponse();
		RegistrationRequest sendModel = new RegistrationRequest();
		sendModel.setEmail(email);
		sendModel.setPassword(password);
		sendModel.setVersion(VERSION_1);
		Gateway returnModel = new Gateway();
		logger.info("Sending register request for: " + email);
		sendPOST(regResp, sendModel, returnModel, new String[]{"service", "registerV1"});
		//System.out.println(">>>>>>>> success=" + regResp.success() + "\n\n" + regResp.getError());
		
		/*
		 * if some type of connection error happened check to see if the registration server URL has changed
		 */
		if (!regResp.success()) {
			if (regResp.getError() != null && (regResp.getError().contains("java.net.UnknownHostException") ||
					regResp.getError().contains("java.net.ConnectException"))) {
				// check Github for current URL
				logger.info("Checking if registration URL has changed");
				//RegisterAccount regAcct = new RegisterAccount();
				RegisterResponse getRegServer = new GetRegisterServerResponse();
				GithubContent githubModel = new GithubContent();
				sendGetReistrationServer(getRegServer, githubModel);
				
				if (getRegServer.hasReturnValue()) {
					logger.info("Message: " + getRegServer.getMessage());
					RegistrationServer server = (RegistrationServer) getRegServer.getReturnValue();
					
					if (!registerServer.equalsIgnoreCase(server.getRegistrationServer().trim())) {
						logger.info("Registration Server URL has changed, trying new URL");
						registerServer = server.getRegistrationServer().trim();
						return registerV1(email, password);
					} else {
						logger.info("Unable to contact registration server: " + registerServer);
					}
					
				} else {
					logger.warning(getRegServer.getError());
				}
			}
		}
		
		return regResp;
	}
	
	
	public RegisterResponse getGatewatConfig() {
		RegisterResponse regResp = new FinishRegistrationResponse();
		Gateway returnModel = new Gateway();
		logger.info("Sending get register config request");
		sendGET(regResp, returnModel, new String[]{"service", "registerV1"});
		
		return regResp;
	}
	/*
	 *  End Version 1 Registration
	 */
	

	/*
	 * Version 2 Registration
	 */
	public RegisterResponse startRegistration(String email, String password) {
		RegisterResponse regResp = new StartRegistrationResponse();
		RegistrationRequest sendModel = new RegistrationRequest();
		sendModel.setEmail(email);
		sendModel.setPassword(password);
		RegistrationUUID returnModel = new RegistrationUUID();
		sendPOST(regResp, sendModel, returnModel, new String[]{"service", "register"});
		
		return regResp;
	}
	
	public RegisterResponse finishRegistration(String uuid) {
		RegisterResponse regResp = new FinishRegistrationResponse();
		RegistrationUUID sendModel = new RegistrationUUID();
		sendModel.setUuid(uuid);
		Gateway returnModel = new Gateway();
		sendPOST(regResp, sendModel, returnModel, new String[]{"service", "finishRegister"});
		
		return regResp;
	}
	
	public static boolean resendEmail(String email) {
		
		return true;
	}
	
	public static boolean startResetPassword(String email) {
		
		return true;
	}
	
	public static boolean finishResetPassword(String password) {
		
		return true;
	}
	/*
	 * End Version 2 registration
	 */
	
	
	public RegisterResponse deleteAccount(String account, String password) {
		
		DeleteAccountResponse regResp = new DeleteAccountResponse();
		DeleteRequest sendModel = new DeleteRequest();
		sendModel.setUser(account);
		sendModel.setPassword(password);
		String returnModel = new String();
		logger.info("Sending delete register account request for account: " + account);
		sendPOST(regResp, sendModel, returnModel, new String[]{"service", "deleteAccount"});
		
		return regResp;
	}
	
	
	private void sendGetReistrationServer(RegisterResponse regResp, Object returnModel) {
		SSLContext ssl;
		
		try {
			ssl = SSLContext.getInstance("TLSv1.2");
		    ssl.init(null, null, null);
		    Client client = ClientBuilder.newBuilder().sslContext(ssl).build();
			Response response = client.target(GITHUB_REG_URL).request().get();
			checkResponse(regResp, returnModel, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	private void sendGET(RegisterResponse regResp, Object returnModel, String[] paths) {
		Invocation.Builder builder = createBuilder(paths);
		Response response = null;
		
		try {
			response = builder.get();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to send GET request: " + e.getMessage(), e);
			e.printStackTrace();
			regResp.setSuccess(false);
			regResp.setError("Error calling register service: " + e.getMessage());
			return;
		}
		
		checkResponse(regResp, returnModel, response);
	}
	
	
	
	private void sendPOST(RegisterResponse regResp, Object sendModel, Object returnModel, String[] paths) {
		
		Invocation.Builder builder = createBuilder(paths);
		ObjectMapper sendMapper = new ObjectMapper();
        Response response = null;
        
        try {
			response = builder.post(Entity.json(sendMapper.writeValueAsString(sendModel)));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to send POST request: " + e.getMessage(), e);
			e.printStackTrace();
			regResp.setSuccess(false);
			regResp.setError("Error parsing request message: " + e.getMessage());
			return;
		}
        
        checkResponse(regResp, returnModel, response);
	}
	
	
	private void checkResponse(RegisterResponse regResp, Object returnModel, Response response) {
		logger.finest("Checking request response");
		
		if (response.getStatus() == 200 && response.hasEntity()) {
			logger.fine("Received a successful response");
	    		String responseString = response.readEntity(String.class);
	    		ObjectMapper mapper = new ObjectMapper();
	    		
	    		try {
	    			
	    			if (returnModel instanceof String) {
	    				returnModel = responseString;
	    			} else {
	    				returnModel = mapper.readValue(responseString, returnModel.getClass());
	    			}
	    			
				regResp.setSuccess(true);
				regResp.setReturnValue(returnModel);
			
	    		} catch (IOException e) {
	    			logger.log(Level.WARNING, "Error parsing response: " + e.getMessage(), e);
				e.printStackTrace();
				regResp.setSuccess(false);
				regResp.setError("Error parsing response: " + e.getMessage());
				response.close();
				return;
			}
	    		
	    		//System.out.println(returnModel);
    		
	    } else {
	    		regResp.setSuccess(false);
	    		
	    		if (response.hasEntity()) {
	    			// assume response is always a String ??
	    			String responseError = response.readEntity(String.class);
	    			regResp.setError("status:" + response.getStatus() + ": Error calling service:\n" + responseError);
	    			logger.warning("Error calling register service, status:" + response.getStatus() + 
	    					":\n" + responseError);
	    		} else {
	    			regResp.setError("status:" + response.getStatus() + ": Error calling service");
	    			logger.warning("Error calling register service, status:" + response.getStatus());
	    		}
	    }
    
		response.close();
	}
	
	
	private Invocation.Builder createBuilder(String[] paths) {
		URI baseURI = getBaseURI();
		Client client = getClient(baseURI.getScheme());
		WebTarget target = client.target(getBaseURI());
        
        // paths[] need to make sure path objects are in the correct order
        for (String path : paths) {
        		logger.finest("Adding path: " + path);
			target = target.path(path);
		}
        
        //System.out.println(target.getUri().getPath());
        return target.request();
	}
	
	
	
	public static void main(String[] args)  throws Exception {
		RegisterAccount regAcct = new RegisterAccount();
		RegisterResponse getRegServer = new GetRegisterServerResponse();
		GithubContent sendModel = new GithubContent();
		regAcct.sendGetReistrationServer(getRegServer, sendModel);
		
		if (getRegServer.hasReturnValue()) {
			System.out.println("Message: " + getRegServer.getMessage());
			RegistrationServer server = (RegistrationServer) getRegServer.getReturnValue();
			System.out.println("Server: " + server.getRegistrationServer());
		} else {
			System.out.println(getRegServer.getError());
		}
		
		
		
		
		
		
		/*
		String getModel = new String();
		regAcct.sendGetReistrationServer(getRegServer, getModel);
		System.out.println(getRegServer.success() + "\n" + getRegServer.getReturnValue());
		String githubContentString = (String) getRegServer.getReturnValue();
		ObjectMapper mapper = new ObjectMapper();
		GithubContent githubContent = mapper.readValue(githubContentString, GithubContent.class);
		System.out.println(githubContent.getContent());
		mapper = new ObjectMapper();
		String content = new String(android.util.Base64.decode(githubContent.getContent(), android.util.Base64.NO_WRAP));
		System.out.println(">\n" + content + "\n>");
		
		RegistrationServer regServer = mapper.readValue(content, RegistrationServer.class);
		System.out.println(regServer.getRegistrationServer());
		*/

		/* 
		Object obj = new Todo();
		Todo sendTodo = new Todo();
		sendTodo.setDescription("D1");
		sendTodo.setSummary("S1");
		ObjectMapper sendMapper = new ObjectMapper();
		
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget target = client.target(getBaseURI()).path("service").path("register");
        Invocation.Builder builder = target.request();
        Response response = builder.post(Entity.json(sendMapper.writeValueAsString(sendTodo)));
        
        System.out.println(response.getStatus());
        
        if (response.getStatus() == 200 && response.hasEntity()) {
        		String responseString = response.readEntity(String.class);
        		ObjectMapper mapper = new ObjectMapper();
        		Object todo = mapper.readValue(responseString, obj.getClass());
        		System.out.println(todo);
        		System.out.println(obj.getClass().getName());
        } else {
        		if (response.hasEntity())
        			System.out.println("Error processing request: " + response.readEntity(String.class));
        		else
        			System.out.println("Error processing request");
        }
        response.close();
        */
		
		
		// version v1 test
		/*
		RegisterAccount registerAccountV1 = new RegisterAccount("localhost:8080");
        RegisterResponse regRespV1 = registerAccountV1.registerV1("test5@mailx.com", "secretPass");
        
        if (regRespV1.success()) {
	    		System.out.println("Success");
	    		System.out.println("GW: " + ((Gateway)regRespV1.getReturnValue()).toString());
	    		System.out.println(regRespV1.getMessage());
	    } else {
	    		System.out.println("error, status: " + regRespV1.getError());
	    }
        
        
        RegisterResponse deleteResponse = registerAccountV1.deleteAccount("test6@mailx.com");
 
        if (deleteResponse.success()) {
	    		System.out.println("Success");
	    		System.out.println("return: " + (deleteResponse.getReturnValue()));
	    		System.out.println(deleteResponse.getMessage());
	    } else {
	    		System.out.println("error, status: " + deleteResponse.getError());
	    }
	    */
    
        
		// version v2 test
        /*
        RegisterAccount registerAccount = new RegisterAccount("localhost:8080");
        RegisterResponse regResp = registerAccount.startRegistration("test@mail.com", "secret_pass");
        
        if (regResp.success()) {
        		System.out.println("Success");
        		System.out.println("UUID: " + ((RegistrationUUID)regResp.getReturnValue()).getUuid());
        		System.out.println(regResp.getMessage());
        } else {
        		System.out.println("error, status: " + regResp.getError());
        }
        
       
        //RegisterResponse finResp = registerAccount.finishRegistration(UUID.randomUUID().toString());
        RegisterResponse finResp = registerAccount.finishRegistration("99");
        
        if (finResp.success()) {
	    		System.out.println("Success");
	    		System.out.println(finResp.getMessage());
	    		System.out.println("GW: " + ((Gateway)finResp.getReturnValue()).toString());
	    } else {
	    		System.out.println("error, status: " + finResp.getError());
	    }
	    */

        /*
        String response = target.path("service").
                            path("register").
                            request().
                            accept(MediaType.APPLICATION_JSON).
                            get(Response.class)
                            .toString();
		

        String plainAnswer =
                target.path("service").path("register").request().accept(MediaType.TEXT_PLAIN).get(String.class);
        String xmlAnswer =
                target.path("service").path("register").request().accept(MediaType.TEXT_XML).get(String.class);
        String htmlAnswer=
                target.path("service").path("register").request().accept(MediaType.TEXT_HTML).get(String.class);
        //String jsonAnswer=
        //        target.path("service").path("register").request().accept(MediaType.APPLICATION_JSON).get(String.class);
        
        Todo sendTodo = new Todo();
        sendTodo.setDescription("Send Description");
        sendTodo.setSummary("Send Summary");
        ObjectMapper objectMapper = new ObjectMapper();
        //String jsonAnswer=
        //        target.path("service").path("register").request().put(Entity.json(objectMapper.readV)).getEntity().toString();

        
        
        
        //System.out.println(response);
        System.out.println(plainAnswer);
        System.out.println(xmlAnswer);
        System.out.println(htmlAnswer);
        //System.out.println(jsonAnswer);
        
        ObjectMapper mapper = new ObjectMapper();
        String s = target.path("service").path("register").request().accept(MediaType.APPLICATION_JSON).get(String.class);
        try {
			Todo todo = mapper.readValue(s, Todo.class);
			System.out.println(todo);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
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

		logger.fine("Rturning client");
		return client;
	}

    private URI getBaseURI() {
    		String protocol = getProtocol();
    		return UriBuilder.fromUri(protocol + "://" + registerServer + BASE_URL).build();
    }
    
    private String getProtocol() {
    		
	    	if (registerServer.contains("8080")) 
	    		return "http";
	    	else 
	    		return "https";
    }
}
