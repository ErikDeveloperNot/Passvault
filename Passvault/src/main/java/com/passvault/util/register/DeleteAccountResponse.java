package com.passvault.util.register;

public class DeleteAccountResponse implements RegisterResponse {
	
	private boolean success;
	private String error = null;
	private String returnMessage = null;
	

	@Override
	public boolean success() {
		return success;
	}

	@Override
	public boolean hasReturnValue() {
		return (error == null ? false : true);
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public String getMessage() {
		if (hasReturnValue())
			return (String)getReturnValue();
		else
			return "";
	}

	@Override
	public Object getReturnValue() {
		return returnMessage;
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
		if (object instanceof String)
			returnMessage = (String)object;
	}

}
