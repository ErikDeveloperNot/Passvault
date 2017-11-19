package com.passvault.util.data.file;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.passvault.crypto.AESEngine;
import com.passvault.util.Account;

public class JsonStoreTest {
	
	JsonStore store = null;
	Account a1;
	Account a2;
	Account a3;
	
	
	@BeforeClass
	public static void beforeClass() {
		System.out.println("Running before class load");
	}
	
	@AfterClass
	public static void afterClass() {
		System.out.println("Running After class destroyed");
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("Running Setup");
		store = new JsonStore();
		
		try {
			store.setEncryptionKey(AESEngine.finalizeKey("key", AESEngine.KEY_LENGTH_256));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long updateTime = System.currentTimeMillis(); 
		a1 = new Account("A1 Account", "a1", "password", "oldpassword", "", updateTime);
		a2 = new Account("A2 Account", "a2", "password", "oldpassword", "", updateTime);
		a3 = new Account("A3 Account", "a3", "password", "oldpassword", "", updateTime);
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("Running teardown");
	}

	@Test
	public void testSaveAccountAccount() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testSaveAccountAccount" );
		store.saveAccount(a1);
	}

	@Test
	public void testSaveAccountAccountString() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testSaveAccountAccountString" );
	}

	@Test
	public void testSaveAccountsListOfAccount() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testSaveAccountsListOfAccount" );
	}

	@Test
	public void testSaveAccountsListOfAccountString() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testSaveAccountsListOfAccountString" );
	}

	@Test
	public void testLoadAccounts() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testLoadAccounts" );
	}

	@Test
	public void testDeleteAccountString() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testLoadAccounts" );
	}

	@Test
	public void testDeleteAccountAccount() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testDeleteAccountAccount" );
	}

	@Test
	public void testSaveAccessMap() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testSaveAccessMap" );
	}

	@Test
	public void testLoadAccessMap() {
		//fail("Not yet implemented");
		System.out.println("Ruuning: testLoadAccessMap" );
	}
	
	// this doesnt test any methods but is a sample how to test on array values
	@Test
	public void testArraySample() {
		int[] orig = {9, 3, 1, 7};
		int[] expected = {1, 3, 7, 9};
		Arrays.sort(orig);
		assertArrayEquals(expected, orig);
	}
	
	// how to run a test for an expected null pointer exception (or any exception)
	@Test(expected=NullPointerException.class)
	public void testNullPointerException() {
		int[] orig = null;
		Arrays.sort(orig);
	}
	
	//one way to test performance, this is pretty basic,
	@Test(timeout=100)
	public void testArray_Performance() {
		int[] array = {1, 3, 7, 9};
		
		for (int i=1; i< 1_000_000; i++) {
			array[0] = i;
			Arrays.sort(array);
		}
	}
	

}
