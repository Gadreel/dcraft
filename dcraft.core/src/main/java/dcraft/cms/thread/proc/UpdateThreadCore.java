package dcraft.cms.thread.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.update.DbRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class UpdateThreadCore implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String tid = UpdateThreadCore.getThreadId(db, params);
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withId(tid)
			.withTable("dcmThread")
			.withConditionallyUpdateFields(params, "Title", "dcmTitle", "EndDate", "dcmEndDate", "TargetDate", "dcmTargetDate", "Originator", "dcmOriginator");
			
		ListStruct lbs = params.getFieldAsList("Labels");
		
		if (!lbs.isEmpty())
			req.withUpdateField("dcmLabels", "|" + StringUtil.join(lbs.toStringList(), "|") + "|");
		
		// TODO update dcmThreadA

		task.getDbm().submit(req, task.getResult());
	}
	
	static public String getThreadId(TablesAdapter db, RecordStruct params) {
		BigDateTime when = BigDateTime.nowDateTime();

		String tid = params.getFieldAsString("Id");

		if (StringUtil.isEmpty(tid) && !params.isFieldEmpty("Uuid")) {
			String uuid = params.getFieldAsString("Uuid");
			
			Object oid = db.firstInIndex("dcmThread", "dcmUuid", uuid, when, false);
			
			if (oid != null)
				tid = oid.toString();
		}

		if (StringUtil.isEmpty(tid) && !params.isFieldEmpty("Hash")) {
			String hash = params.getFieldAsString("Hash");
			
			Object oid = db.firstInIndex("dcmThread", "dcmHash", hash, when, false);
			
			if (oid != null)
				tid = oid.toString();
		}
		
		return tid;
	}
}
