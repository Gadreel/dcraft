package dcraft.db.trigger;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class BeforeUserUpdate implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		RecordStruct params = task.getParamsAsRecord();
		String id = params.getFieldAsString("Id");
		
		if (StringUtil.isEmpty(id)) {
			log.error("User id required to update user.");
			return;
		}		
		
		RecordStruct fields = params.getFieldAsRecord("Fields");
		RecordStruct uname = fields.getFieldAsRecord("dcUsername");
		
		// nothing to check
		if (uname == null)
			return;
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		try {
			for (FieldStruct fs : uname.getFields()) {
				RecordStruct rec = (RecordStruct) fs.getValue();
				
				if (rec.isFieldEmpty("Data")) {
					log.error("Username may not be empty.");
					return;
				}
				
				Object userid = db.firstInIndex("dcUser", "dcUsername", rec.getFieldAsString("Data"), BigDateTime.nowDateTime(), false);
		
				if (userid != null) {
					String uid = userid.toString();

					if (!id.equals(uid)) {
						log.error("Username must be unique, this username (email) already in use.");
						return;
					}
				}
			}
		}
		catch (Exception x) {
			log.error("Update User: Failed to read Index: " + x);
		}
	}
}
