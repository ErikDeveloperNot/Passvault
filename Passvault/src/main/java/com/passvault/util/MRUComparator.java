package com.passvault.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.passvault.util.couchbase.CBLStore;

public class MRUComparator implements Comparator<Account> {

	/*
	 * do a lazy load if document doesn't exist in db yet just let it get created as account is accessed
	 * or when a sort is done
	 * 
	 * MRU = most-recently-used, this should really be most obvious used in the last 35 days, with the smallest 
	 * interval taking precedence
	 */
	
	private static final int[] INTERVALS = {7, 14, 35};
	private static final long DAY = 86_400_000;
	private static Logger logger;
	
	private static MRUComparator mruComaparater;
	private long CURRENT_DAY;
	private Map<String, AccountAccessMap> accountAccessMaps;
	private boolean reverse;
	private CBLStore cblStore;
	
	
	static {
		logger = Logger.getLogger("com.passvault.util");
	}
	
	
	public static MRUComparator getInstance() {
		// no worries with concurrency in single threaded UI
		if (mruComaparater == null)
			mruComaparater = new MRUComparator();
		
		return mruComaparater;
	}
	
	
	public MRUComparator(CBLStore cblStore) {
		this.cblStore = cblStore;
		CURRENT_DAY = getDay();
		Collection accounts = cblStore.loadAccessMap();
		accountAccessMaps = new HashMap<>();
		
		if (accounts != null) {
			logger.fine("Loading accounts");
				
			for (Object account : accounts) {
				Map<String, Object> fields = (Map)account;
				String name = (String)fields.get("name");
				long mraTime = ((Number)fields.get("mraTime")).longValue();
				List mapAsObject = (List)fields.get("map");
				int[] map = new int[mapAsObject.size()];
				int i = 0;

				for (Object mapValue : mapAsObject) 
					map[i++] = (Integer)mapValue;
			
				accountAccessMaps.put(name, new AccountAccessMap(name, mraTime, map));
				logger.finest("Account: " + name + " loaded, last mraTime: " + new Date(mraTime) + "\n" + map);
			}
		} 
	}
	
	
	private MRUComparator() {
		accountAccessMaps = new HashMap<>();
		CURRENT_DAY = getDay();
	}
	
	
	public void accountAccessed(String accountName) {
		AccountAccessMap acm = accountAccessMaps.get(accountName);
		logger.fine("Account: " + accountName + ", accessed");
		
		if (acm == null) {
			logger.finest("creating new account map");
			acm = new AccountAccessMap(accountName, 0L);
			accountAccessMaps.put(accountName, acm);
		} else {
			acm.setMap(shiftMap(acm.getMraTime(), acm.getMap()));
			logger.finest("loaded existing account map");
		}
				
		acm.getMap()[0]++;
		acm.mraTime = CURRENT_DAY;
	}
	
	
	public void accountAdded(String accountName) {
		logger.fine("Adding Account: " + accountName);
		accountAccessMaps.put(accountName, new AccountAccessMap(accountName, 0L));
	}
	
	
	public void accountRemoved(String accountName) {
		logger.fine("Removing Account: " + accountName);
		accountAccessMaps.remove(accountName);
	}
	
	
	
	
	
	public void saveAccessMap(CBLStore cblStore) {
		logger.info("Saving Account access map");
		cblStore.saveAccessMap(accountAccessMaps.values());
	}
	
	
	public void setReverse(boolean reverse) {
		// set if the list should be reversed when sorted
		logger.finest("Reverse accout list set to: " + reverse);
		this.reverse = reverse;
	}
	
	
	private long getDay() {
		Calendar now = Calendar.getInstance();
		now.set(Calendar.HOUR, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		return now.getTimeInMillis();
	}
	
	
	private int[] shiftMap(long time, int[] map) {
		
		int numberToShift = (int)((CURRENT_DAY - time) / DAY);
		int size = INTERVALS[INTERVALS.length - 1];
		logger.fine("Number to shift: " + numberToShift + ", size: " + size + ", shift map: " + (numberToShift > 0));
		
		if (numberToShift > 0) {
			logger.finest("Current map: \n" + map);
			
			if (numberToShift < size) {
				int startIndex = size - numberToShift - 1;
				
				for (; startIndex >= 0; startIndex--)
					map[startIndex + numberToShift] = map[startIndex];
				
				for (int i=0; i<numberToShift; i++)
					map[i] = 0;
				
			} else {
				map = new int[size];
			}
		}
			
		logger.finest("Returning map:\n" + map);
		return map;
	}
	
	
	@Override
	public int compare(Account o1, Account o2) {
		logger.finest("Comparing accounts: [" + o1.getName() + ", " + o2.getName() + "]");
		AccountAccessMap o1Map = accountAccessMaps.get(o1.getName());
		AccountAccessMap o2Map = accountAccessMaps.get(o2.getName());
		long today = getDay();
		
		if (o1Map == null) {
			o1Map = new AccountAccessMap(o1.getName(), 0L);
			accountAccessMaps.put(o1.getName(), o1Map);
		}
	
		if (o2Map == null) {
			o2Map = new AccountAccessMap(o2.getName(), 0L);
			accountAccessMaps.put(o2.getName(), o2Map);
		}
		
		// start with finest interval and return if one is greater then the other
		for (int interval : INTERVALS) {
			logger.finest("Checking interval: " + interval);
			int o1Cnt = 0;
			int o2Cnt = 0;
			
			if ((o1Map.getMraTime() + interval * DAY) >= CURRENT_DAY) 
				for (int i=0; i<interval; i++) 
					o1Cnt += o1Map.getMap()[i];
				
			if ((o2Map.getMraTime() + interval * DAY) >= CURRENT_DAY) 
				for (int i=0; i<interval; i++) 
					o2Cnt += o2Map.getMap()[i];
					
			logger.finest(o1.getName() + " total: " + o1Cnt + ", " + o2.getName() + " total: " + o2Cnt);
			
			if (o1Cnt > o2Cnt)
				if (reverse)
					return 1;
				else
					return -1;
			
			if (o2Cnt > o1Cnt)
				if (reverse)
					return -1;
				else
					return 1;
		}
		
		// both have same hit counts, now sort by Alpha
		if (reverse)
			return o1.compareTo(o2) * -1;
		else
			return o1.compareTo(o2);
	}
	

	private String printMap(int[] map) {
		String toReturn = "[";
		
		for (int i : map) {
			toReturn += i + " ";
		}
		
		return toReturn += "]";
	}
	
	
	public class AccountAccessMap {
		private String name;
		private long mraTime;
		private int[] map;
		
		public AccountAccessMap(String accountName, long lastAccessTime, int[] map) {
			name = accountName;
			mraTime = lastAccessTime;
			this.map = map;
			logger.finest("Access map created for: " + accountName + ", map:\n" + map);
		}
		
		public AccountAccessMap(String accountName, long lastAccessTime) {
			this(accountName, lastAccessTime, new int[INTERVALS[INTERVALS.length - 1]]);
			
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
		
	}
	
	
	public static void main(String args[]) {
		Calendar now = Calendar.getInstance();
		System.out.println(now.getTimeInMillis());
		now.set(Calendar.HOUR, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		System.out.println(now.getTimeInMillis());
		System.out.println(now);
		
		int[] map = new int[35];
		for (int i=0; i < map.length; i++) {
			map[i] = i;
			System.out.print(i + " ");
		}
		//System.out.println(printMap(map));
		

		int size = 35;
		int numberToShift = 33;
			
		if (numberToShift < size) {
			int startIndex = size - numberToShift - 1;

			for (; startIndex >= 0; startIndex--)
				map[startIndex + numberToShift] = map[startIndex];

			for (int i = 0; i < numberToShift; i++)
				map[i] = 0;

		} else {
			map = new int[size];
		}
		
		System.out.println("");
		
		for (int i : map) {
			System.out.print(i + " ");
		}
		//System.out.println(printMap(map));
	}

}
