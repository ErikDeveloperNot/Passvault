package com.passvault.util.data.couchbase;

import com.couchbase.lite.Database;
import com.couchbase.lite.Database.ChangeEvent;
import com.couchbase.lite.DocumentChange;



/*
 *  If client is sync'd with more then 1 GW can get into a situation where the same
 *  account with different docID may exist, as well as docs with a different accountUUID's
 *  being pulled in.
 *  
 *  1. when accounts with a different accountUUID are brought in they need to be saved with the new 
 *  accountUUID
 *  2. if a document with the same docID with the new accountUUID already exists will check 
 *  the updateTime to pick current revision
 *  
 */
public class DuplicateAccountDatabaseChangeListener implements Database.ChangeListener {
	
	private String accountUUID;
	
	
	public DuplicateAccountDatabaseChangeListener(String accountUUID) {
		this.accountUUID = accountUUID;
	}

	@Override
	public void changed(ChangeEvent event) {
		System.out.println("DuplicateAccountDatabaseChangeListener received change event: ");
		
		for (DocumentChange change : event.getChanges()) {
			System.out.println("docID: " + change.getDocumentId() + ", is conflict: " + change.isConflict());
		}
		
		
	}

}
