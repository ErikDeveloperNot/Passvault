package com.passvault.util.model;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Gateways {
	
	/*
	 		"remote", Gateway,
	 		"local", Gateways[] 
	 */
			

	private Gateway remote;
	private Gateway[] local;
	
	public Gateway getRemote() {
		return remote;
	}
	public void setRemote(Gateway remote) {
		this.remote = remote;
	}
	public Gateway[] getLocal() {
		return local;
	}
	public void setLocal(Gateway[] local) {
		this.local = local;
	}
	
	
	
	public static void main(String args[]) throws Exception {
		byte[] jsonData = Files.readAllBytes(Paths.get("/Users/user1/.passvault/.password_vault_sync.json"));
		String s = new String(jsonData);
		ObjectMapper objectMapper = new ObjectMapper();
		Gateways gateWays = objectMapper.readValue(s, Gateways.class);
		
		Gateway remote = gateWays.getRemote();
		
		if (remote != null) {
			System.out.println(remote.getProtocol() + "://" + remote.getServer() + ":" + remote.getPort() + "\n" +
						remote.getBucket() + "\n" + remote.getUserName() + "\n" +
						remote.getPassword());
			
			System.out.println(s);
		}
		
		Gateway[] local = gateWays.getLocal();
		
		if (local != null && local.length > 0) {
			
			for (Gateway gw : local) {
				System.out.println(gw.getProtocol() + "://" + gw.getServer() + ":" + gw.getPort() + "\n" +
						gw.getBucket() + "\n" + gw.getUserName() + "\n" +
						gw.getPassword());
				
				System.out.println(s);
			}
		}
	}
}
