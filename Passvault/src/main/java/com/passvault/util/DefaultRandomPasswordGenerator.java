package com.passvault.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DefaultRandomPasswordGenerator implements RandomPasswordGenerator {
	
	private static Logger logger;
	protected List<Character> allowedCharacters;
	protected int length;
	protected boolean lower, upper, special, digits;
	
	protected char[] specials = {'@', '_', '$', '&', '!', '?', '*', '-'};
	
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
		this.lower = lower;
		this.upper = upper;
		this.special = special;
		this.digits = digits;
		allowedCharacters = new ArrayList<>();
		
		//set allowed lowercase
		if (lower)
			for (int x=((int)'a'); x<=((int)'z'); x++)
				allowedCharacters.add((char)x);
		
		//set allowed uppercase
		if (upper)
			for (int x=((int)'A'); x<=((int)'Z'); x++)
				allowedCharacters.add((char)x);
		
		//set allowed numbers
		if (digits)
			for (int x=((int)'0'); x<=((int)'9'); x++)
				allowedCharacters.add((char)x);
		
		//set allowed special characters
		if (special) {
			char[] c = {'@', '_', '$', '&', '!', '?', '*', '-'};
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
			
			if (lower && check >= (int)'a' && check <= (int)'z') {
				lowerMet = true;
			} else if (upper && check >= (int)'A' && check <= (int)'Z') {
				upperMet = true;
			} else if (digits && check >= (int)'0' && check <= (int)'9') {
				digitMet = true;
			} else {
				if (special && allowedCharacters.contains((char)check)) {
					specialMet = true;
				}
			}
		}

		//verify password contraints are met
		public boolean constraintsEnforced() {
			boolean keepChecking = true;
			//System.out.println("lower="+lowerMet+",upper="+upperMet+",digit="+digitMet+",special="+specialMet);;
			
			if (lower)
				keepChecking = lowerMet ? true : false;
			
			if (keepChecking && upper)
				keepChecking = upperMet ? true : false;
			
			if (keepChecking && digits)
				keepChecking = digitMet ? true : false;
			
			if (keepChecking && special)
				keepChecking = specialMet ? true : false;
			
			return keepChecking ? true : false;
		}
		
	}
}
