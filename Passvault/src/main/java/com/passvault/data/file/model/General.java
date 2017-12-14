package com.passvault.data.file.model;

import java.util.Map;

public class General {

	private boolean saveKey;
	private boolean sortMRU;
	private String accountUUID;
	private String key;
	
	public General() {
		saveKey = false;
		sortMRU = true;
		accountUUID = "";
		key = "";
	}
	
	public General(Map<String, Object> values) {
		saveKey = (Boolean)values.get("saveKey");
		sortMRU = (Boolean) values.get("sortMRU");
		accountUUID = (String) values.get("accountUUID");
		key = (String) values.get("key");
		
		if (accountUUID == null)
			accountUUID = "";
		
		if (key == null)
			key = "";
	}
	
	
	public boolean isSaveKey() {
		return saveKey;
	}
	public void setSaveKey(boolean saveKey) {
		this.saveKey = saveKey;
	}
	public boolean isSortMRU() {
		return sortMRU;
	}
	public void setSortMRU(boolean sortMRU) {
		this.sortMRU = sortMRU;
	}
	public String getAccountUUID() {
		return accountUUID;
	}
	public void setAccountUUID(String accountUUID) {
		this.accountUUID = accountUUID;
	}

	@Override
	public String toString() {
		return "{\n saveKey: " + saveKey + "\n sortMRU: " + sortMRU + "\n accountUUID: " + accountUUID + "\n}";
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	
}
