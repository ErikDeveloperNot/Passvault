package com.passvault.util.model.syncserver;

/*
  account_name    varchar(1000)      NOT NULL ,
  account_uuid          varchar(320)    NOT NULL ,
  user_name       varchar(1000)      NOT NULL ,
  password        varchar(1000)      NOT NULL ,
  old_password    varchar(1000)      NOT NULL ,
  url             text                   NULL ,
  update_time     bigint             NOT NULL ,
  deleted		   boolean	 	NOT NULL
 */


public class Account {
	
	private String accountName;
	private String userName;
	private String password;
	private String oldPassword;
	private String url;
	private long updateTime;
	private boolean deleted;
	
	public Account() {
		super();
	}
	
	public Account(String accountName, String userName, String password, String oldPassword, String url,
			long updateTime, boolean deleted) {
		super();
		this.accountName = accountName;
		this.userName = userName;
		this.password = password;
		this.oldPassword = oldPassword;
		this.url = url;
		this.updateTime = updateTime;
		this.deleted = deleted;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	/*
	@Override
	public boolean equals(Object obj) {
		boolean superResponse = super.equals(obj);
		
		System.out.println("Checking against object type: " + obj.getClass().getName());
		if (obj instanceof CheckAccount) {
			return ((CheckAccount)obj).getAccountName().equalsIgnoreCase(accountName) ? true : false;
		} else {
			return superResponse;
		}
	}
	*/

}
