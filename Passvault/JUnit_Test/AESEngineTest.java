package com.passvault.util.data.file;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.passvault.crypto.AESEngine;

@RunWith(Parameterized.class)
public class AESEngineTest {
	
	private String key;
	private String finalKey;
	private AESEngine engine;

	public AESEngineTest(String key, String finalKey) {
		engine = new AESEngine();
		this.key = key;
		this.finalKey = finalKey;
	}
	
	
	@Parameters
	public static Collection<String[]> keysToTest() {
		String keys[][] = { {"key1", "key1T0NQ'0I,Fd)ln$HxbB_XF4x-\\Cnh"}, 
				{"key2", "key2/\\\\D$cbxv`P6AX2c#08Y&*p5Fh$r" }, 
				{"key3", "key3/\\3q(iKXVi\",HiQxEBJWl*wX_jl8"} };
		return Arrays.asList(keys);
	}

	@Test
	public void runParameterizedTest() {
		//fail("Not yet implemented");
		try {
			System.out.println(engine.finalizeKey(key, AESEngine.KEY_LENGTH_256));
			assertEquals(finalKey, engine.finalizeKey(key, AESEngine.KEY_LENGTH_256));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
