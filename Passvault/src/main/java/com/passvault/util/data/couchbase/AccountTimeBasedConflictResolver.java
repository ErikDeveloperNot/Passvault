package com.passvault.util.data.couchbase;

import java.util.List;
import java.util.logging.Logger;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.LiveQuery.ChangeEvent;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;

public class AccountTimeBasedConflictResolver implements LiveQuery.ChangeListener {

	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.util.couchbase");
	}
	
	public AccountTimeBasedConflictResolver() {
		super();
	}
	
	@Override
	public void changed(ChangeEvent event) {
		QueryEnumerator enumerator = event.getRows();
		QueryRow row;
		logger.finest("Changes count = " + enumerator.getCount());
		
		while ((row = enumerator.next()) != null) { 
			List<SavedRevision> revs = row.getConflictingRevisions();
			logger.finest("# of Conflicts=" + revs.size());
			
			if (revs.size() < 1)
				continue;
			
			int winner = 0; int count = 0;
			long updateTime = 0L;
			
			for (SavedRevision savedRevision : revs) {
				
				Object updateObj = savedRevision.getProperty("UpdateTime");
				long time;
				
				if (updateObj == null)   // old account type, not current
					continue;
				
				if (updateObj instanceof Double)
					time = ((Double)updateObj).longValue();
				else
					time = (Long)updateObj;
				
				if (time > updateTime) {
					winner = count;
					updateTime = time;
				}
				
				count++;
			}
			
			try {
				revs.remove(winner);
				
				for (SavedRevision savedRevision : revs) {
					UnsavedRevision delRev = savedRevision.createRevision();
					delRev.setIsDeletion(true);
					logger.finest("Deleting Revision: " + savedRevision.toString());
					delRev.save();
				}
				
			} catch (CouchbaseLiteException e) {
				e.printStackTrace();
			}
		}
		
		
	}

}
