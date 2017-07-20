package com.passvault.util.register;

import com.passvault.util.model.Gateway;

public class FinishRegistrationResponse implements RegisterResponse {
	
	private static final String SUCCESS_RESPONSE = "Your account has been setup and is ready for you to sync " +
			"your passwords.";
	
	private boolean success;
	private String error;
	private Gateway gateway;


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
		
		if (object instanceof Gateway)
			this.gateway = (Gateway) object;
	}

	@Override
	public boolean success() {
		return success;
	}

	@Override
	public boolean hasReturnValue() {
		
		if (success) 
			return true;
		else
			return false;
	}

	@Override
	public String getError() {
		
		if (!success)
			return error;
		else
			return null;
	}

	@Override
	public String getMessage() {
		
		if (success)
			return SUCCESS_RESPONSE;
		else
			return error;
	}

	@Override
	public Object getReturnValue() {
		
		if (success)
			return gateway;
		else
			return null;
	}

}
