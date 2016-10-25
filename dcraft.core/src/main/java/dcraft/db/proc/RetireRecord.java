package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public class RetireRecord implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		String id = params.getFieldAsString("Id");
		
		// TODO add db filter option
		//d runFilter("Retire") quit:Errors  ; if any violations in filter then do not proceed
		
		TablesAdapter db = new TablesAdapter(conn, task); 

		db.setStaticScalar(table, id, "Retired", true);
		
		task.complete();
	}
}
