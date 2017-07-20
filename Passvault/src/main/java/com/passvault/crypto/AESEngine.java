package com.passvault.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESEngine implements CryptEngine {
	
	/*
	 * 
	 * crypto stuff copied from  @author www.codejava.net from
	 * http://www.codejava.net/coding/file-encryption-and-decryption-simple-example
	 * 
	 */
	
	private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    //public static final int KEY_LENGTH_64 = 8;
    public static final int KEY_LENGTH_128 = 16;
    public static final int KEY_LENGTH_192 = 24;
    public static final int KEY_LENGTH_256 = 32;
    //public static final int KEY_LENGTH_512 = 64;
    
    private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.crypto");
	}

	@Override
	public String decryptFile(String key, File file) {
		logger.finest("Decrypting file: " + file.getName());
		Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
		String decryptedContent = null;
		FileInputStream fis = null;
		
		try {
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.DECRYPT_MODE, secretKey);
	        byte[] encryptedBytes = new byte[(int) file.length()];
	        fis = new FileInputStream(file);
	        fis.read(encryptedBytes);
	        logger.finest("encryptedBytes length: " + encryptedBytes.length);
	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        logger.finest("decryptedBytes length: " + decryptedBytes.length);
	        decryptedContent = new String(decryptedBytes);
	        fis.close();
	
		} catch (Exception e) {
			logger.severe("Error decrypting file: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch(IOException ioe) {}
		}
		
		return decryptedContent;
	}
	

	@Override
	public String decryptString(String key, String encryptedString) {
		
		if (encryptedString.length() == 0)
			return "";
		
		Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
		String decryptedContent = null;
		logger.finest("encryptedString length: " + encryptedString.length());		
		
		try {
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.DECRYPT_MODE, secretKey);
	        
	        byte[] encryptedBytes = new byte[encryptedString.length()];
	        encryptedBytes = encryptedString.getBytes();
	        //byte[] encryptedBytes = encryptedString.getBytes();
	        logger.finest("encryptedBytes length: " + encryptedBytes.length);
	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        logger.finest("decryptedBytes length: " + decryptedBytes.length);
	        decryptedContent = new String(decryptedBytes);
	
		} catch (Exception e) {
			logger.severe("Error decrypting password: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
		return decryptedContent;
	}
	
	
	@Override
	public String decryptBytes(String key, byte[] encryptedBytes) throws Exception {
		
		if (encryptedBytes.length%16 != 0 || encryptedBytes.length == 0)
			return "";
		
		Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
		String decryptedContent = null;
		logger.finest("encryptedBytes length: " + encryptedBytes.length);		
		
		try {
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.DECRYPT_MODE, secretKey);
	        
	        //byte[] encryptedBytes = new byte[encryptedString.length()];
	        //encryptedBytes = encryptedString.getBytes();
	        //byte[] encryptedBytes = encryptedString.getBytes();

	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        logger.finest("decryptedBytes length: " + decryptedBytes.length);
	        decryptedContent = new String(decryptedBytes);
		} catch (Exception e) {
			logger.severe("Error decrypting bytes: " + e.getMessage());
			e.printStackTrace();
			//System.exit(-1);
			throw new Exception("Error dycrpting, " + e.getMessage());
		}
		
		return decryptedContent;
	}


	@Override
	public byte[] encryptString(String key, String unencryptedString) {
		
		if (unencryptedString == null || unencryptedString.length() == 0)
			return null;
		
		Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
		String encryptedContent = null;
		
		try {
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
	        
	        byte[] unencryptedBytes = unencryptedString.getBytes();
	        logger.finest("unencryptedBytes length: " + unencryptedBytes.length);	        
	        byte[] encryptedBytes = cipher.doFinal(unencryptedBytes);
	        logger.finest("encryptedBytes length: " + encryptedBytes.length);	        
	        return encryptedBytes;
	        
		} catch (Exception e) {
			logger.severe("Error encrypting password: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
		return null;
	}



	public static String finalizeKey(String key, int length) throws Exception {
		//todo revisit, but keep it simple just to get it working and pad with the same char
		int amountToPad = 0;
		logger.info("finalizing key of length: " + key.length() + ", to a key length of: " + length);
		
		switch (length) {
		case KEY_LENGTH_192:
		case KEY_LENGTH_128:
		case KEY_LENGTH_256:	
		//case KEY_LENGTH_512:
		
			
			if (key.length() > length) 
				amountToPad = length - key.length();
			else if (key.length() < length)
				amountToPad = length - key.length();
			
			logger.finest("Amount to pad key: " + amountToPad);

			break;
		default:
			throw new InvalidSpecifiedKeyLengthException();
		}
		
		if (amountToPad > 0) {
			// zero padded
			/*
			for (int i=0; i<amountToPad; i++)
				key += "0";
			*/
			
			// no idea if this is better, no math/crypt skills but seems like it would produce a better spread
			StringBuilder builder = new StringBuilder(key);
			int mod = 256;
			
			for (int i=1; i<= amountToPad; i++) {
				int i1 = i * (int)key.charAt(i%key.length());
				int i2 = (i+3*i) * (int)key.charAt((i+2)%key.length());
				int i3 = ((i1 * i2) % mod);
				int x = 3;
				
				while ((i3 < 65 || i3 > 122) && (i3 < 34 || i3 > 57)) 
					i3 = ((i1 * x++) % mod--);
				
				builder.append((char)i3);
			}
			
			key = builder.toString();
			
		} else if (amountToPad < 0) {
			key = key.substring(0, key.length() - amountToPad*-1);
		}
		
		logger.info("Returning key of length: " + key.length());
		return key;
	}


	public static CryptEngine getInstance() {
		logger.finest("Returning new instance of AESEngine");
		return new AESEngine();
	}
	
	
	public static class InvalidSpecifiedKeyLengthException extends Exception {

		@Override
		public String getMessage() {
			// TODO Auto-generated method stub
			return "Invalid Key length specified, allowed values " + KEY_LENGTH_128 + ", " + KEY_LENGTH_192 +
					", " + KEY_LENGTH_256 + "\n";
		}
		
	}
	
	/*
	public static void main(String args[]) throws Exception {
	
		String key = "key1key1key1key1";
		
		AESEngine aes = new AESEngine();
		String encryptMe = "EncryptThis";
		byte[] encrypted = aes.encryptString(key, encryptMe);
		System.out.println("encrypted bytes length=" + encrypted.length);
		String encryptedString = new String(encrypted);
		System.out.println("encryptedString length=" + encryptedString.length());
		String decryptedString = aes.decryptBytes(key, encrypted);
		
		System.out.println("decryptedString length=" + decryptedString);
		
		System.out.println(AESEngine.finalizeKey(key, KEY_LENGTH_128));
		System.out.println(AESEngine.finalizeKey(key, KEY_LENGTH_192));
		System.out.println(AESEngine.finalizeKey(key, KEY_LENGTH_256));
		//System.out.println(aes.finalizeKey(key, KEY_LENGTH_512));
	}
	*/

}
