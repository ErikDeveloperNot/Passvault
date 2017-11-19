package com.passvault.util;

import java.util.logging.Logger;

//import com.passvault.util.MRUComparator.AccountAccessMap;

public class AccountAccessMap {
	private String name;
	private long mraTime;
	private int[] map;
	
	private static Logger logger;
	
	
	static {
		logger = Logger.getLogger("com.passvault.util");
	}
	
	
	public AccountAccessMap() {}
	
	public AccountAccessMap(String accountName, long lastAccessTime, int[] map) {
		name = accountName;
		mraTime = lastAccessTime;
		this.map = map;
		logger.finest("Access map created for: " + accountName + ", map:\n" + map);
	}
	
	public AccountAccessMap(String accountName, long lastAccessTime) {
		this(accountName, lastAccessTime, new int[MRUComparator.INTERVALS[MRUComparator.INTERVALS.length - 1]]);
		
		/* for testing
		System.out.println(this.map.length);
		Random r = new Random();
		for (int i=0; i<map.length; i++) {
			map[i] = r.nextInt() % 12;
			map[i] = (map[i] < 0) ? map[i] * -1 : map[i] * 1;
			map[i] = i;
			System.out.println(map[i]);
		}
		mraTime = getDay();
		
		if (accountName.contains("O1"))
			map[34] = 2;
		// end testing */
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getMraTime() {
		return mraTime;
	}

	public void setMraTime(long mraTime) {
		this.mraTime = mraTime;
	}

	public int[] getMap() {
		return map;
	}

	public void setMap(int[] map) {
		this.map = map;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof AccountAccessMap && ((AccountAccessMap)obj).getName().equalsIgnoreCase(name))
			return true;
		else
			return false;
	}
	
	
	
}
