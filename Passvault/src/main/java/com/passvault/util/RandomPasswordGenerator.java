package com.passvault.util;

import java.util.List;

public interface RandomPasswordGenerator {

	List<Character> getAllowedCharactres();
	void setAllowedCharacters(char addition);
	void removedAllowedCharacters(char remove);
	String generatePassword();
	String generatePassword(int length);
}
