package com.passvault.crypto;

import java.io.File;

public interface CryptEngine {

	String decryptFile(String key, File file);
	String decryptString(String key, String encryptedString) throws Exception;
	byte[] encryptString(String key, String unencryptedString);
	String decryptBytes(String key, byte[] encryptedBytes) throws Exception;
	//verify key length and modify returning new key if needed. Some implementations can just return say key
	//String finalizeKey(String key, int length) throws Exception;
}
