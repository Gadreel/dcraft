package dcraft.cms.thread.proc;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.update.DbRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

public class ChangeStatusAction implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String tid = UpdateThreadCore.getThreadId(db, params);
		boolean read = params.getFieldAsBoolean("Read");
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withId(tid)
			.withTable("dcmThread");
			
		ListStruct plist = params.getFieldAsList("Parties");
		
		for (int i = 0; i < plist.getSize(); i++) { 
			req.withUpdateField("dcmRead", plist.getItemAsString(i), read);
			req.withUpdateField("dcmLastRead", plist.getItemAsString(i), read ? new DateTime() : null);
		}
		
		/* TODO  5) read status changed for party
			- set in dcRecord for that party, do not change Modified
			- update dcmThreadA for that thread id
			- set dcmThreadB party+new folder = null - do so for all labels, party labels and "star"         (means force recalc)
		 */
		
		task.getDbm().submit(req, task.getResult());
	}
}
