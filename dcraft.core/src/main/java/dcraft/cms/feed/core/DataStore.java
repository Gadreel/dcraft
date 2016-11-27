package dcraft.cms.feed.core;

import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectResult;
import dcraft.db.ReplicatedDataRequest;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationCallback;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;

// TODO abstract so that other data stores may be used instead of dcDb
public class DataStore {
	static public void updateFeed(RecordStruct params, OperationCallback cb) {
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			cb.complete();
			return;
		}
		
		db.submit(
			new ReplicatedDataRequest("dcmFeedUpdate2")
				.withParams(params), 
			new ObjectResult() {
				@Override
				public void process(CompositeStruct result3b) {
					cb.complete();
				}
			});
	}
	
	static public void deleteFeed(String path, OperationCallback cb) {
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			cb.complete();
			return;
		}
		
		db.submit(
			new ReplicatedDataRequest("dcmFeedDelete2")
				.withParams(new RecordStruct()
					.withField("Path", path)
				), 
			new ObjectResult() {
				@Override
				public void process(CompositeStruct result3b) {
					cb.complete();
				}
			});
	}
}
