package com.passvault.util.register;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passvault.model.GithubContent;
import com.passvault.model.RegistrationServer;

import android.util.Base64;

public class GetRegisterServerResponse implements RegisterResponse {

	private boolean success;
	private String error;
	
	private String message = null;
	private RegistrationServer server;
	
	@Override
	public boolean success() {
		return success;
	}

	@Override
	public boolean hasReturnValue() {
		return (server != null) ? true : false;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public String getMessage() {
		if (success)
			return message;
		else
			return error;
	}

	@Override
	public Object getReturnValue() {
		return server;
	}

	@Override
	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public void setError(String error) {
		this.error = error;
	}

	@Override
	public void setReturnValue(Object object) {
		
		if (object instanceof GithubContent) {
			GithubContent githubContent = (GithubContent)object;
			ObjectMapper mapper = new ObjectMapper();
			// set this since the x509 will have unquoted new lines
			mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		
			try {
				server = mapper.readValue(new String(Base64.decode(githubContent.getContent(), Base64.NO_WRAP)),
						RegistrationServer.class);
			} catch (Exception e) {
				e.printStackTrace();
				success = false;
				setError("Error get content from Github: " + e.getMessage());
			} 
			
			message = "Retrieved registration server: " + server.getRegistrationServer();
		}
	}

}
