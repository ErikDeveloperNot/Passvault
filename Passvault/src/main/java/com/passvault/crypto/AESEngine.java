package com.passvault.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.Provider;
import java.security.Security;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
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
    private static final String CIPHER = "AES/CBC/PKCS5Padding";
    
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
	

	public String decryptStringDeprecated(String key, String encryptedString) {
		
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
			//System.exit(-1);
		}
		
		return decryptedContent;
	}
	
	
	@Override
	public String decryptString(String key, String encryptedString) throws Exception {
		return this.decryptBytes(key, encryptedString.getBytes("UTF-8"));
	}
	
	
	public String decryptBytesDeprecated(String key, byte[] encryptedBytes) throws Exception {
		
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
			throw new Exception("Error dycrpting, " + e.getMessage());
		}
		
		return decryptedContent;
	}
	
	
	@Override
	public String decryptBytes(String key, byte[] encryptedBytes) throws Exception {
		String decrypted = null;
		
		if (encryptedBytes == null || encryptedBytes.length == 0)
			return decrypted;
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        		String ivString = finalizeKey(key.substring(20, 24), 16);
String ivString2 = "0123456789012345";
//System.out.println("iv: " + ivString);
//System.out.println("iv: " + ivString2);
ivString = ivString2;
            IvParameterSpec iv = new IvParameterSpec(ivString.getBytes("UTF-8"));
        		
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            decrypted = new String(cipher.doFinal(encryptedBytes));
            
        } catch (Exception e) {
        		logger.severe("Error decrypting password: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error dycrpting, " + e.getMessage());
        }
        
        return decrypted;
	}


	public byte[] encryptStringDeprecated(String key, String unencryptedString) {
		
		if (unencryptedString == null || unencryptedString.length() == 0)
			return null;
		
		Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
		
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
			//System.exit(-1);
		}
		
		return null;
	}
	
	
	@Override
	public byte[] encryptString(String key, String unencryptedString) {
		byte[] encrypted = null;
		
		if (unencryptedString == null || unencryptedString.length() == 0)
			return null;
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        	String ivString = finalizeKey(key.substring(20, 24), 16);
 String ivString2 = "0123456789012345";
//System.out.println("iv: " + ivString); 
// System.out.println("iv: " + ivString2);
 ivString = ivString2;
            IvParameterSpec iv = new IvParameterSpec(ivString.getBytes("UTF-8"));
        		
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            encrypted = cipher.doFinal(unencryptedString.getBytes());
            
        } catch (Exception e) {
        		logger.severe("Error encrypting password: " + e.getMessage());
			e.printStackTrace();
			//System.exit(-1);
        }
            
        return encrypted;
    }



	public static String finalizeKey(String key, int length) throws Exception {
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
//System.out.println(i1 + " " + i2 + " " + i3);
				while ((i3 < 65 || i3 > 122) && (i3 < 34 || i3 > 57)) 
					i3 = ((i1 * x++) % mod--);
				
				builder.append((char)i3);
			}
			
			key = builder.toString();
			
		} else if (amountToPad < 0) {
			key = key.substring(0, key.length() - amountToPad*-1);
		}
//System.out.println("KEY=" + key);
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
	
	
	public static void main(String args[]) throws Exception {
	
		String key = "key1key1key1key1";
		
		AESEngine aes = new AESEngine();
		/*String encryptMe = "EncryptThis";
		byte[] encrypted = aes.encryptString(key, encryptMe);
		System.out.println("encrypted bytes length=" + encrypted.length);
		String encryptedString = new String(encrypted);
		System.out.println("encryptedString length=" + encryptedString.length());
		String decryptedString = aes.decryptBytes(key, encrypted);
		
		System.out.println("decryptedString length=" + decryptedString);
		
		System.out.println(AESEngine.finalizeKey(key, KEY_LENGTH_128));
		System.out.println(AESEngine.finalizeKey(key, KEY_LENGTH_192));
		System.out.println(AESEngine.finalizeKey("notreal", KEY_LENGTH_256));*/
		//System.out.println(aes.finalizeKey(key, KEY_LENGTH_512));
		
		System.out.println("\nTesting decryption with IV");
//		key = "TestKey";
		key = "01234567890123456789012345678901";
		key = aes.finalizeKey(key, KEY_LENGTH_256);
		System.out.println("final key=" + key);
		System.out.println("key length=" + key.length());
//		String plainText = "The quick brown fox jumps over the lazy dog";
		String plainText = "password";
		byte[] enc1 = aes.encryptString(key, plainText);
		String enc1Encoded = Base64.getEncoder().encodeToString(enc1);
		String enc2Encoded = android.util.Base64.encodeToString(enc1, android.util.Base64.NO_WRAP);
		System.out.println("enc1Encoded=" + enc1Encoded + "|, length=" + enc1Encoded.length());
		System.out.println("enc12Encoded=" + enc2Encoded + "|");
		System.out.println("Bytes: " + new String(enc1));
		System.out.println("decrypt bytes=" + aes.decryptBytes(key, enc1));
		//System.out.println("decrypt string=" + aes.decryptString(key, new String(enc1)));
		System.out.println("HelloPlaygroundHelloAgainHello".substring(20, 24));
	}
	

}
