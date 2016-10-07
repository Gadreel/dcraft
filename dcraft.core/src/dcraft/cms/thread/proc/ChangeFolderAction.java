package dcraft.cms.thread.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.update.DbRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

public class ChangeFolderAction implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String tid = UpdateThreadCore.getThreadId(db, params);
		String folder = params.getFieldAsString("Folder");
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withId(tid)
			.withTable("dcmThread");
			
		ListStruct plist = params.getFieldAsList("Parties");
		
		for (int i = 0; i < plist.getSize(); i++) 
			req.withUpdateField("dcmFolder", plist.getItemAsString(i), folder);

		/* TODO  3) folder changed for party
			- set in dcRecord for that party, do not change Modified
			- kill old dcmThreadA that thread id
			- set new dcmThreadA for that thread id
			- set dcmThreadB party+old folder = null - do so for all labels, party labels and "star"         (means force recalc)
			- set dcmThreadB party+new folder = null - do so for all labels, party labels and "star"         (means force recalc)
		 */
		
		task.getDbm().submit(req, task.getResult());
	}
}
