package com.passvault.data.file.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passvault.data.file.JsonStore;
import com.passvault.util.AccountAccessMap;
import com.passvault.util.MRUComparator;

public class Store {

	private double format = 1.1;
	private int version = 1;
	//private String accountUUID;
	//private AccountModel[] accounts;
	//private AccountAccessMap[] mraMaps;
	private Map<String, AccountModel> accounts;
	private Map<String, AccountAccessMap> mraMaps;
	private Settings settings;


	public double getFormat() {
		return format;
	}
	public void setFormat(double format) {
		this.format = format;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	/*public String getAccountUUID() {
		return accountUUID;
	}
	public void setAccountUUID(String accountUUID) {
		this.accountUUID = accountUUID;
	}*/
	
	/*
	public AccountModel[] getAccounts() {
		return accounts;
	}
	public void setAccounts(AccountModel[] accounts) {
		this.accounts = accounts;
	}
	public AccountAccessMap[] getMraMaps() {
		return mraMaps;
	}
	public void setMraMaps(AccountAccessMap[] mraMaps) {
		this.mraMaps = mraMaps;
	}
	*/
	public Settings getSettings() {
		return settings;
	}
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	public Map<String, AccountModel> getAccounts() {
		return accounts;
	}
	public void setAccounts(Map<String, AccountModel> accounts) {
		this.accounts = accounts;
	}
	public Map<String, AccountAccessMap> getMraMaps() {
		return mraMaps;
	}
	public void setMraMaps(Map<String, AccountAccessMap> mraMaps) {
		this.mraMaps = mraMaps;
	}
	
	
}
