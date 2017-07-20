package com.passvault.util.register;

/*
 * Interface for all REST call responses
 */
public interface RegisterResponse {
	//call was successful
	boolean success();
	//call returned Object value
	boolean hasReturnValue();
	//get error from call, success should return false if this is set
	String getError();
	//get returned message, should always be set, even when error is returned
	String getMessage();
	//returned Value, hasReturnValue() should return true
	Object getReturnValue();
	
	//set success
	void setSuccess(boolean success);
	//set error
	void setError(String error);
	//set return value
	void setReturnValue(Object object);
}
