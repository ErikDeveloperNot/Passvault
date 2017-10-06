package com.passvault.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DefaultRandomPasswordGenerator implements RandomPasswordGenerator {
	
	private static Logger logger;
	protected List<Character> allowedCharacters;
	protected int length;
	protected boolean checkLower, checkUpper, checkSpecial, checkDigits;
	
	protected char[] specials = {'@', '_', '$', '&', '!', '?', '*', '-'};
	private char[] defaultLower;
	private char[] defaultUpper;
	private char[] defaultDigits;
	private char[] defaultSpecial;
	
	static {
		logger = Logger.getLogger("com.passvault.util");
	}
	
	public DefaultRandomPasswordGenerator() {
		this(32, true, true, true, true);
	}

	public DefaultRandomPasswordGenerator(Integer length, Boolean lower, Boolean upper, 
			Boolean special, Boolean digits) {
		
		super();
		
		this.length = length;
		this.checkLower = lower;
		this.checkUpper = upper;
		this.checkSpecial = special;
		this.checkDigits = digits;
		allowedCharacters = new ArrayList<>();
		
		//set allowed lowercase
		if (lower) {
			defaultLower = new char[26];
			
			for (int x=((int)'a'), j=0; x<=((int)'z'); x++, j++) {
				allowedCharacters.add((char)x);
				defaultLower[j] = (char)x;
			}
		}
		
		//set allowed uppercase
		if (upper) {
			defaultUpper = new char[26];
			
			for (int x=((int)'A'), j=0; x<=((int)'Z'); x++, j++) {
				allowedCharacters.add((char)x);
				defaultUpper[j] = (char)x;
			}
		}
		
		//set allowed numbers
		if (digits) {
			defaultDigits = new char[10];
			
			for (int x=((int)'0'), j=0; x<=((int)'9'); x++, j++) {
				allowedCharacters.add((char)x);
				defaultDigits[j] = (char)x;
			}
		}
		
		//set allowed special characters
		if (special) {
			char[] c = {'@', '_', '$', '&', '!', '?', '*', '-'};
			defaultSpecial = c;
			
			for (char d : c) {
				allowedCharacters.add(d);
			}
		}
	}

	@Override
	public List<Character> getAllowedCharactres() {
		return allowedCharacters;
	}

	@Override
	public void setAllowedCharacters(char addition) {
		if (!allowedCharacters.contains(addition))
			allowedCharacters.add(addition);
	}

	@Override
	public void removedAllowedCharacters(char remove) {
		allowedCharacters.remove(new Character(remove));
	}

	@Override
	public String generatePassword() {
		return this.generatePassword(length);
	}

	@Override
	public String generatePassword(int length) {
		
		StringBuilder stringBuilder = new StringBuilder();
		Random random = null;
		
		try {
			random = SecureRandom.getInstanceStrong();
			logger.fine("Using SecureRandom for generator");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Not able to use SecureRandom, falling back to RandomNumberGenerator !!!!");
			logger.log(Level.WARNING, "Unable to use SecureRandom, using RandomNumberGenerator: " + e.getMessage(), e);
			e.printStackTrace();
			random = ThreadLocalRandom.current();
		}
		
		/*
		System.out.println("Algorithm: " + ((SecureRandom)random).getAlgorithm() + ",  Provider: " +
				((SecureRandom)random).getProvider().getName() + ", " + ((SecureRandom)random).getProvider().getInfo() +
				", " + ((SecureRandom)random).getProvider().getVersion());
		*/

		PasswordConstraints constraints = null;
		
		do {
			constraints = new PasswordConstraints();
			int x = 0;
			stringBuilder = new StringBuilder();
			
			while (++x <= length) {
				int next=random.nextInt();
				char checkChar = allowedCharacters.get(Math.abs(next%allowedCharacters.size()));
				stringBuilder.append(checkChar);
				constraints.checkValue(checkChar);
			}
		
		} while (!constraints.constraintsEnforced());
		
		return stringBuilder.toString();
	}

	
	public static RandomPasswordGenerator getInstance() {
		//default password allows all characters and is 32 characters in length
		return new DefaultRandomPasswordGenerator(32, true, true, true, true);
	}

	
	public static RandomPasswordGenerator getInstance(int length, boolean lower, boolean upper, boolean special,
			boolean digits) {
		return new DefaultRandomPasswordGenerator(length, lower, upper, special, digits);
	}
	
	
	public class PasswordConstraints {
		//flag if constraint is present
		private boolean lowerMet = false;
		private boolean upperMet = false;
		private boolean specialMet = false;
		private boolean digitMet = false;
		
		//is constraint enforced
		/*
		private boolean allowLower = false;
		private boolean allowUpper = false;
		private boolean allowSpecial = false;
		private boolean allowDigit = false;
		*/
		
		//private List<Character> allowedSpecial = new ArrayList<>();

		
		/*
		PasswordConstraints(boolean allowLower, boolean allowUpper, boolean allowSpecial, 
				boolean allowDigit, List<Character> allowedSpecial) {
			super();
			this.allowLower = allowLower;
			this.allowUpper = allowUpper;
			this.allowSpecial = allowSpecial;
			this.allowDigit = allowDigit;
			this.allowedSpecial = allowedSpecial;
		}
		*/


		//flip appropriate flag for generated value
		public void checkValue(char value) {
			int check = (int)value;
			
			if (checkLower && check >= (int)'a' && check <= (int)'z') {
				lowerMet = true;
			} else if (checkUpper && check >= (int)'A' && check <= (int)'Z') {
				upperMet = true;
			} else if (checkDigits && check >= (int)'0' && check <= (int)'9') {
				digitMet = true;
			} else {
				if (checkSpecial && allowedCharacters.contains((char)check)) {
					specialMet = true;
				}
			}
		}

		//verify password contraints are met
		public boolean constraintsEnforced() {
			boolean keepChecking = true;
			//System.out.println("lower="+lowerMet+",upper="+upperMet+",digit="+digitMet+",special="+specialMet);;
			
			if (checkLower)
				keepChecking = lowerMet ? true : false;
			
			if (keepChecking && checkUpper)
				keepChecking = upperMet ? true : false;
			
			if (keepChecking && checkDigits)
				keepChecking = digitMet ? true : false;
			
			if (keepChecking && checkSpecial)
				keepChecking = specialMet ? true : false;
			
			return keepChecking ? true : false;
		}
		
	}


	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public char[] getDefaultLower() {
		return defaultLower;
	}

	public char[] getDefaultUpper() {
		return defaultUpper;
	}

	public char[] getDefaultDigits() {
		return defaultDigits;
	}

	public char[] getDefaultSpecial() {
		return defaultSpecial;
	}
	
	public void clearAllowedCharacters() {
		allowedCharacters.clear();
	}
	
	public void setAllowedCharacters(Collection<Character> c) {
		allowedCharacters.addAll(c);
	}
	/*
	public void checkLower(boolean check) {
		checkLower = check;
	}
	
	public void checkUpper(boolean check) {
		checkUpper = check;
	}
	
	public void checkDigits(boolean check) {
		checkDigits = check;
	}
	
	public void checkSpecial(boolean check) {
		checkSpecial = check;
	}
	*/
	public boolean isCheckLower() {
		return checkLower;
	}

	public void setCheckLower(boolean checkLower) {
		this.checkLower = checkLower;
	}

	public boolean isCheckUpper() {
		return checkUpper;
	}

	public void setCheckUpper(boolean checkUpper) {
		this.checkUpper = checkUpper;
	}

	public boolean isCheckSpecial() {
		return checkSpecial;
	}

	public void setCheckSpecial(boolean checkSpecial) {
		this.checkSpecial = checkSpecial;
	}

	public boolean isCheckDigits() {
		return checkDigits;
	}

	public void setCheckDigits(boolean checkDigits) {
		this.checkDigits = checkDigits;
	}
}
