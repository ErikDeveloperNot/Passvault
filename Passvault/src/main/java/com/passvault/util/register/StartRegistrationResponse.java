package com.passvault.util.register;

import com.passvault.util.model.RegistrationUUID;

public class StartRegistrationResponse implements RegisterResponse {

	private static final String SUCCESS_RESPONSE = "A unique identifier has been sent to your email in order " +
			"to finish the registration process.";
	
	private boolean success;
	private String error;
	private RegistrationUUID registrationUUID;
	
	
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

		if (object instanceof RegistrationUUID)
			this.registrationUUID = (RegistrationUUID) object;
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

		return registrationUUID;
	}

}
