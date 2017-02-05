package dcraft.cms.thread.trigger;

import java.util.List;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.op.OperationResult;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

public class BeforeThreadUpdate implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		RecordStruct fields = params.getFieldAsRecord("Fields");
		boolean idxfnd = false;
		
		for (FieldStruct field : fields.getFields()) {
			String fname = field.getName();
			
			if ("dcmModified".equals(fname) || "dcmFolder".equals(fname) || "dcmRead".equals(fname)) {
				idxfnd = true;
				break;
			}
		}
		
		if (! idxfnd)
			return;
		
		String id = params.getFieldAsString("Id");
		TablesAdapter db = new TablesAdapter(conn, task); 

		ListStruct oldparties = new ListStruct();
		RecordStruct oldvalues = new RecordStruct()
				.withField("dcmModified", db.getStaticScalar("dcmThread", id, "dcmModified"))
				.withField("Parties", oldparties);
		
		List<String> parties = db.getStaticListKeys("dcmThread", id, "dcmFolder");
		
		for (String party : parties) {
			String folder = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
			
			//Boolean isread = (Boolean) db.getStaticList("dcmThread", id, "dcmRead", party);
			
			oldparties.withItems(new RecordStruct()
				.withField("dcmParty", party)
				.withField("dcmFolder", folder)
				//.withField("dcmRead", isread)
			);
		}		
		
		params.withField("__OldValues", oldvalues);
	}
}
