package dcraft.cms.feed.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.update.RetireRecordRequest;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public class DeleteFeed implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		//String chann = params.getFieldAsString("Channel");
		String path = params.getFieldAsString("Path");
		
		// TODO replicating
		// if (task.isReplicating())
		
		// TODO delete from dcmFeedIndex too
		
		// TODO support sites
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		BigDateTime when = BigDateTime.nowDateTime();
		Object oid = db.firstInIndex("dcmFeed", "dcmPath", path, when, false);
		
		if (oid != null) {
			RetireRecordRequest lr1 = new RetireRecordRequest("dcmFeed", oid.toString());
			
			task.getDbm().submit(lr1, task.getResult());
		}
		else {
			task.complete();
		}
	}
}
