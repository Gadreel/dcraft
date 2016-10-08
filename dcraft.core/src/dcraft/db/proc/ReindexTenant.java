package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationResult;

public class ReindexTenant implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		TablesAdapter db = new TablesAdapter(conn, task);
		
		db.rebuildIndexes();
		
		Hub.instance.getTenantInfo(task.getTenant()).fireAfterReindex();
		
		task.complete();
	}
}
