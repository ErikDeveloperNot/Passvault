package com.passvault.data.file.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.passvault.util.DefaultRandomPasswordGenerator;

public class Properties {

	//private Character[] allowedCharacters;
	private List<Character> allowedCharacters;
	private int length;
	
	public Properties() {
		length = 32;
		allowedCharacters = new DefaultRandomPasswordGenerator().getAllowedCharactres();
		//allowedCharacters = new DefaultRandomPasswordGenerator().getAllowedCharactres().toArray(new Character[0]);
	}
	
	public Properties(Map<String, Object> values) {
		length = (Integer)values.get("length");
		//allowedCharacters = stringArrayToCharArray((String[])((List)values.get("allowedCharacters")).toArray(new String[0]));
		allowedCharacters = new ArrayList<>();
		
		for (String s : (List<String>)values.get("allowedCharacters")) {
			//System.out.println(allowedCharacters + ", " + s);
			allowedCharacters.add(s.charAt(0));
		}
		
		//allowedCharacters = (List)values.get("allowedCharacters");
	}
	
	//public Character[] getAllowedCharacters() {
	public List<Character> getAllowedCharacters() {
		return allowedCharacters;
	}
	
	//public void setAllowedCharacters(Character[] allowedCharacters) {
	public void setAllowedCharacters(List<Character> allowedCharacters) {
		this.allowedCharacters = allowedCharacters;
	}
	
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public String toString() {
		return "{\n allowedCharacters: " + allowedCharacters.toString() + "\n length: " + length + "\n}";
	}
	
	
	private Character[] stringArrayToCharArray(String[] in) {
		Character[] out = new Character[in.length];
		
		for (int i=0; i<in.length; i++)
			out[i] = in[i].charAt(0);
		
		return out;
	}
	
}
